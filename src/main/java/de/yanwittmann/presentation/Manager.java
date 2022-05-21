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

    private final List<User> users = new ArrayList<>();

    private final String adminPassword;

    public Manager() {
        this.adminPassword = randomAlphanumericString(10);
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

    public void startupServer(int webSocketPort, int httpPort) {
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

    public void onUserMessage(String message) {
        if (message.startsWith("{")) {

            JSONObject json = new JSONObject(message);
            String from = json.optString("from", null);
            String content = json.optString("content", null);
            boolean adminPasswordCorrect = json.optString("admin", "").equals(adminPassword);

            if (from != null && content != null) {
                User user = findOrCreateUser(from);

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

    public void onUserReactionChange(User user, Reaction reaction) {
        broadcastToAllUsers(new JSONObject()
                .put("type", "updateUsers")
                .put("users", new JSONArray().put(user.toJson())));
    }

    private void sendMessageToUser(User user, Object message) {
        JSONObject json = new JSONObject();
        json.put("to", user.getName());
        json.put("content", message);
        webSocketServer.broadcast(json.toString());
    }

    private void broadcastToAllUsers(Object message) {
        JSONObject json = new JSONObject();
        json.put("to", "ALL_USERS");
        json.put("content", message);
        webSocketServer.broadcast(json.toString());
    }

    private User findOrCreateUser(String name) {
        for (User user : users) {
            if (user.getName().equals(name)) {
                return user;
            }
        }

        User user = new User(name);
        user.addReactionChangeListener(this::onUserReactionChange);
        users.add(user);

        return user;
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
