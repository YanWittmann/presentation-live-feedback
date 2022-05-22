package de.yanwittmann.presentation;

import com.sun.net.httpserver.HttpServer;
import de.yanwittmann.presentation.data.Reaction;
import de.yanwittmann.presentation.data.User;
import de.yanwittmann.presentation.websocket.EventServer;
import de.yanwittmann.presentation.websocket.EventSocket;
import de.yanwittmann.presentation.websocket.WebsiteHttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Manager {

    private final static Logger LOG = LoggerFactory.getLogger(Manager.class);

    private final EventServer webSocketServer = new EventServer();
    private HttpServer httpServer;
    private final int webSocketPort, httpPort;

    private final List<User> users = new ArrayList<>();

    private final String adminPassword;

    public Manager(int webSocketPort, int httpPort) {
        this.webSocketPort = webSocketPort;
        this.httpPort = httpPort;
        this.adminPassword = randomAlphanumericString(10);
    }

    public Manager(int webSocketPort, int httpPort, String adminPassword) {
        this.webSocketPort = webSocketPort;
        this.httpPort = httpPort;
        this.adminPassword = adminPassword;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public String randomAlphanumericString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    public void startupServer() {
        EventSocket.addUserMessageListener(this::onUserMessage);

        new Thread(() -> {
            try {
                webSocketServer.setPort(webSocketPort);
                webSocketServer.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();

        try {
            httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);
            httpServer.createContext("/presentation", new WebsiteHttpHandler(this));
            httpServer.setExecutor(null);
            httpServer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getWebSocketPort() {
        return webSocketPort;
    }

    public void onUserMessage(String message) {
        if (message.startsWith("{")) {

            JSONObject json = new JSONObject(message);
            String fromUUID = json.optString("uuid", null);
            String from = json.optString("from", null);
            String content = json.optString("content", null);
            boolean adminPasswordCorrect = json.optString("admin", "").equals(adminPassword);

            if (fromUUID != null && from != null && content != null) {
                User user = findOrCreateUser(fromUUID, from);

                if (content.equals("register")) {
                    broadcastToAllUsers(generateReactionsMessage());
                    broadcastToAllUsers(generateUserInformationMessage());
                } else if (content.startsWith("{")) {
                    JSONObject messageJson = new JSONObject(content);
                    switch (messageJson.optString("type", "")) {
                        case "reaction":
                            String emote = messageJson.optString("emote", null);
                            String reactionName = messageJson.optString("reactionName", null);
                            if (emote != null && reactionName != null) {
                                Reaction reaction = Reaction.findReaction(reactionName, emote);
                                user.setReaction(reaction);
                            }
                            break;
                        case "handRaiseToggle":
                            userHandRaiseToggle(user);
                            break;
                        case "adminRemoveHandRaise":
                            if (adminPasswordCorrect) {
                                User foundUser = findUser(messageJson.optString("userUUID", ""), messageJson.optString("userName", ""));
                                if (foundUser != null) {
                                    userHandLower(foundUser);
                                }
                            } else {
                                sendMessageToUser(user, new JSONObject().put("type", "modal").put("title", "Unauthorized").put("message", "You do not have the permission to perform this action."));
                            }
                            break;
                        case "clearAllHandRaise":
                            if (adminPasswordCorrect) {
                                users.forEach(this::userHandLower);
                            } else {
                                sendMessageToUser(user, new JSONObject().put("type", "modal").put("title", "Unauthorized").put("message", "You do not have the permission to perform this action."));
                            }
                            break;
                        case "clearAllReactions":
                            if (adminPasswordCorrect) {
                                users.forEach(u -> u.setReaction(Reaction.REACTIONS.get(0)));
                            } else {
                                sendMessageToUser(user, new JSONObject().put("type", "modal").put("title", "Unauthorized").put("message", "You do not have the permission to perform this action."));
                            }
                            break;
                        case "adminMessage":
                            if (adminPasswordCorrect) {
                                String sendMessage = messageJson.optString("message", null);
                                String toUserUUID = messageJson.optString("userUUID", null);
                                String toUserName = messageJson.optString("userName", null);
                                        String fromUser = messageJson.optString("from", null);
                                if (sendMessage != null && toUserUUID != null && fromUser != null) {
                                    JSONObject sendMessageJson = new JSONObject().put("type", "modal").put("title", "Message from " + fromUser).put("message", sendMessage);
                                    if (toUserUUID.equals("ALL_USERS")) {
                                        broadcastToAllUsers(sendMessageJson);
                                    } else {
                                        User foundUser = findUser(toUserUUID, toUserName);
                                        if (foundUser != null) {
                                            sendMessageToUser(foundUser, sendMessageJson);
                                        }
                                    }
                                }
                            } else {
                                sendMessageToUser(user, new JSONObject().put("type", "modal").put("title", "Unauthorized").put("message", "You do not have the permission to perform this action."));
                            }
                            break;
                        case "isAdminPasswordCorrect":
                            sendMessageToUser(user, new JSONObject().put("type", "adminConfirmation").put("isCorrect", adminPasswordCorrect));
                            break;
                    }
                }
            } else {
                LOG.warn("Message does not specify from and content: {}", message);
            }
        } else {
            LOG.warn("Received message is not a JSON Object {}", message);
        }
    }

    public void onHandRaised(User user, int handRaisedIndex) {
        LOG.info("Hand index set to {} by {}", handRaisedIndex, user.getName());
        broadcastToAllUsers(new JSONObject()
                .put("type", "updateUsers")
                .put("users", new JSONArray().put(user.toJson())));
    }

    private void userHandRaiseToggle(User user) {
        if (user.isHandRaised()) {
            userHandLower(user);
        } else {
            userHandRaise(user);
        }
    }

    private void userHandRaise(User user) {
        if (user.isHandRaised()) return;
        int handRaiseCount = (int) users.stream().filter(User::isHandRaised).count();
        user.setHandRaisedIndex(handRaiseCount + 1);
        reorderHandRaised();
    }

    private void userHandLower(User user) {
        if (!user.isHandRaised()) return;
        user.setHandRaisedIndex(-1);
        reorderHandRaised();
    }

    private boolean handRaiseIndexExists(int index) {
        return users.stream().anyMatch(user -> user.isHandRaised() && user.getHandRaisedIndex() == index);
    }

    private void reorderHandRaised() {
        int currentHandRaiseCount = 1;
        int handRaiseCount = (int) users.stream().filter(User::isHandRaised).count();
        while (currentHandRaiseCount <= handRaiseCount) {
            if (!handRaiseIndexExists(currentHandRaiseCount)) {
                final int finalCurrentHandRaiseCount = currentHandRaiseCount;
                users.stream().filter(user -> user.isHandRaised() && user.getHandRaisedIndex() > finalCurrentHandRaiseCount).forEach(user -> user.setHandRaisedIndex(user.getHandRaisedIndex() - 1));
            }
            currentHandRaiseCount++;
        }
    }

    public void onUserReactionChange(User user, Reaction reaction) {
        broadcastToAllUsers(new JSONObject()
                .put("type", "updateUsers")
                .put("users", new JSONArray().put(user.toJson())));
    }

    private void sendMessageToUser(User user, Object message) {
        JSONObject json = new JSONObject();
        json.put("to", user.getName());
        json.put("toUUID", user.getUuid());
        json.put("content", message);
        webSocketServer.broadcast(json.toString());
    }

    private void broadcastToAllUsers(Object message) {
        JSONObject json = new JSONObject();
        json.put("to", "ALL_USERS");
        json.put("content", message);
        webSocketServer.broadcast(json.toString());
    }

    private User findOrCreateUser(String uuid, String name) {
        for (User user : users) {
            if (user.isUser(uuid, name)) {
                return user;
            }
        }

        User user = new User(uuid, name);
        user.addReactionChangeListener(this::onUserReactionChange);
        user.addHandRaisedListener(this::onHandRaised);
        users.add(user);

        return user;
    }

    private User findUser(String uuid, String name) {
        for (User user : users) {
            if (user.isUser(uuid, name)) {
                return user;
            }
        }
        return null;
    }

    private JSONObject generateReactionsMessage() {
        return new JSONObject()
                .put("type", "updateReactions")
                .put("reactions", Reaction.REACTIONS.stream().map(Reaction::toJson).collect(Collectors.toList()));
    }

    private JSONObject generateUserInformationMessage() {
        return new JSONObject()
                .put("type", "updateUsers")
                .put("users", users.stream().map(User::toJson).collect(Collectors.toList()));
    }


    public void shutdownServer() throws Exception {
        webSocketServer.stop();
        httpServer.stop(0);
    }
}
