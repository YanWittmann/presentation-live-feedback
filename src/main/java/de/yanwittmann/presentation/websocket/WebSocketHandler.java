package de.yanwittmann.presentation.websocket;

import de.yanwittmann.presentation.model.internal.Emotion;
import de.yanwittmann.presentation.model.internal.Session;
import de.yanwittmann.presentation.model.internal.SessionParticipant;
import de.yanwittmann.presentation.model.internal.SessionState;
import de.yanwittmann.presentation.service.SessionService;
import de.yanwittmann.presentation.service.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketHandler extends TextWebSocketHandler {

    private final static Logger LOG = LogManager.getLogger(WebSocketHandler.class);

    private final UserService userService;
    private final SessionService sessionService;
    private final Map<WebSocketSession, SessionParticipant> claimedChannels = new ConcurrentHashMap<>();
    private final Set<WebSocketSession> allChannels = ConcurrentHashMap.newKeySet();

    public WebSocketHandler(UserService userService, SessionService sessionService) {
        this.userService = userService;
        this.sessionService = sessionService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        LOG.info("New WS connection from {}", session.getRemoteAddress());
        this.allChannels.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        LOG.info("WS connection closed from {}", session.getRemoteAddress());
        unregisterChannel(session);
    }

    @Override
    public void handleTextMessage(WebSocketSession channel, TextMessage message) {
        try {
            final JSONObject payload = new JSONObject(message.getPayload());

            final String type = payload.getString("type");

            if (type.equals("register")) {
                final String userId = payload.optString("userId", null);
                final String sessionName = payload.optString("sessionName", null);

                if (this.claimedChannels.containsKey(channel)) {
                    LOG.warn("Session {} already registered, changing registration", channel);
                    unregisterChannel(channel);
                }

                final SessionParticipant participant = this.userService.findUserById(UUID.fromString(userId));
                if (participant == null) {
                    LOG.warn("Unknown user id [{}] in message {} from {}", userId, message.getPayload(), channel);
                    return;
                }

                this.claimedChannels.put(channel, participant);

                if (sessionName == null) {
                    LOG.info("Registered management channel [{}] for user {}", channel, participant);
                    participant.enterNoSession(channel);

                } else {
                    final Session session = this.sessionService.findSessionByName(sessionName);
                    if (session == null) {
                        LOG.warn("Unknown session id [{}] in message {} from channel [{}] for user {}", sessionName, message.getPayload(), channel, participant);
                        return;
                    }
                    participant.enterSession(session, channel);
                }

            } else if (type.startsWith("session")) {
                final SessionParticipant participant = claimedChannels.get(channel);

                if (participant == null) {
                    LOG.warn("WebSocket channel has not yet been claimed in message {} from {}", message.getPayload(), channel);
                    return;
                }

                final Session session = participant.findSessionByChannel(channel);
                if (session == null) {
                    LOG.warn("WebSocket channel has not been assigned to a session in message {} from channel [{}] for user {}", message.getPayload(), channel, participant);
                    return;
                }

                final SessionState sessionState = participant.getSessionState(session);

                if (type.equals("session-state-emotion")) {
                    final String emotion = payload.getString("emotion");
                    final Emotion emotionValue;
                    try {
                        emotionValue = Emotion.valueOf(emotion);
                    } catch (Exception e) {
                        LOG.warn("Unknown emotion [{}] in message {} from channel [{}] for user {}", emotion, message.getPayload(), channel, participant);
                        return;
                    }
                    sessionState.setEmotion(emotionValue);

                } else if (type.equals("session-state-toggle-hand")) {
                    final boolean isHandRaised = sessionState.getRaisedHandTime() != null;
                    if (isHandRaised) {
                        sessionState.setRaisedHandTime(null);
                    } else {
                        sessionState.setRaisedHandTime(new Date());
                    }

                } else if (type.equals("session-manager-remove-user")) {
                    final String affectedUserId = payload.optString("affectedUserId", null);
                    final String messageText = payload.optString("message", null);

                    if (!participant.isSuperuser()) {
                        LOG.warn("User {} wanted remove user {} from session {}, but is not a superuser",
                                participant, affectedUserId, session);
                        return;
                    }

                    LOG.info("Authorized user {} removing user {} from session {}",
                            participant, affectedUserId, session);

                    final SessionParticipant affectedUser = userService.findUserByReferenceId(UUID.fromString(affectedUserId));
                    if (affectedUser == null) {
                        LOG.warn("User {} wanted remove user {} from session {}, but affected user does not exist",
                                participant, affectedUserId, session);
                        return;
                    }

                    affectedUser.sendToWebSocketChannelsBySession(session, new JSONObject()
                            .put("type", "kicked-from-session")
                            .put("message", "You were removed from the session by " + participant.getUsername() + (messageText == null ? "." : " with the following reason: " + messageText))
                    );
                    session.removeParticipant(affectedUser);
                } else if (type.equals("session-manager-send-message-to-user")) {
                    final String affectedUserId = payload.optString("affectedUserId", null);
                    final String messageText = payload.optString("message", null);

                    if (!participant.isSuperuser()) {
                        LOG.warn("User {} wanted message user {} from session {}, but is not a superuser",
                                participant, affectedUserId, session);
                        return;
                    }

                    LOG.info("Authorized user {} messaging user {} from session {}",
                            participant, affectedUserId, session);

                    final SessionParticipant affectedUser = userService.findUserByReferenceId(UUID.fromString(affectedUserId));

                    affectedUser.sendToWebSocketChannelsBySession(session, new JSONObject()
                            .put("type", "display-message")
                            .put("message", participant.getUsername() + " sent you a message: " + messageText)
                    );

                } else if (type.equals("session-manager-send-message-to-all-users-in-session")) {
                    final String messageText = payload.optString("message", null);

                    if (!participant.isSuperuser()) {
                        LOG.warn("User {} wanted message alls users from session {}, but is not a superuser",
                                participant, session);
                        return;
                    }

                    LOG.info("Authorized user {} messaging all users from session {}",
                            participant, session);

                    for (SessionParticipant affectedUser : session.getParticipants()) {
                        affectedUser.sendToWebSocketChannelsBySession(session, new JSONObject()
                                .put("type", "display-message")
                                .put("message", participant.getUsername() + " sent you a message: " + messageText)
                        );
                    }
                } else if (type.equals("session-manager-set-timer")) {
                    final String wsSetTimer = payload.optString("countToDate", null);

                    if (!participant.isSuperuser()) {
                        LOG.warn("User {} wanted set timer from session {}, but is not a superuser",
                                participant, session);
                        return;
                    }

                    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                    final Date date = dateFormat.parse(wsSetTimer);

                    LOG.info("Authorized user {} set timer from session {}",
                            participant, session);

                    session.setTimerTargetDate(date);

                    for (SessionParticipant affectedUser : session.getParticipants()) {
                        affectedUser.sendToWebSocketChannelsBySession(session, new JSONObject()
                                .put("type", "timer-changed")
                                .put("timerTargetDate", date.getTime())
                        );
                    }

                } else if (type.equals("session-manager-remove-hand-raised-from-user")) {
                    final String affectedUserId = payload.optString("affectedUserId", null);

                    if (!participant.isSuperuser()) {
                        LOG.warn("User {} wanted remove hand raised from user {} from session {}, but is not a superuser",
                                participant, affectedUserId, session);
                        return;
                    }

                    final SessionParticipant affectedUser = userService.findUserByReferenceId(UUID.fromString(affectedUserId));
                    if (affectedUser == null) {
                        LOG.warn("User {} wanted remove hand raised from user {} from session {}, but affected user does not exist",
                                participant, affectedUserId, session);
                        return;
                    }

                    if (!session.getParticipants().contains(affectedUser)) {
                        LOG.warn("User {} wanted remove hand raised from user {} from session {}, but affected user is not part of session",
                                participant, affectedUserId, session);
                        return;
                    }

                    LOG.info("Authorized user {} removing hand raised from user {} from session {}",
                            participant, affectedUser, session);

                    final SessionState affectedUserSessionState = affectedUser.getSessionState(session);
                    affectedUserSessionState.setRaisedHandTime(null);

                } else if (type.equals("session-manager-move-user-to-session")) {
                    final String affectedUserId = payload.optString("affectedUserId", null);
                    final String targetSessionId = payload.optString("targetSessionId", null);

                    if (!participant.isSuperuser()) {
                        LOG.warn("User {} wanted move user {} to session {}, but is not a superuser",
                                participant, affectedUserId, targetSessionId);
                        return;
                    }

                    LOG.info("Authorized user {} moving user {} to session {}",
                            participant, affectedUserId, targetSessionId);

                    final SessionParticipant affectedUser = userService.findUserByReferenceId(UUID.fromString(affectedUserId));
                    if (affectedUser == null) {
                        LOG.warn("User {} wanted move user {} to session {}, but affected user does not exist",
                                participant, affectedUserId, targetSessionId);
                        return;
                    }

                    final Session targetSession = sessionService.findSessionById(UUID.fromString(targetSessionId));
                    if (targetSession == null) {
                        LOG.warn("User {} wanted move user {} to session {}, but session does not exist",
                                participant, affectedUserId, targetSessionId);
                        return;
                    }

                    targetSession.addParticipant(affectedUser);

                    affectedUser.sendToWebSocketChannelsBySession(session, new JSONObject()
                            .put("type", "move-to-session")
                            .put("targetSessionName", targetSession.getName())
                            .put("movedby", participant.getUsername())
                    );

                    affectedUser.leaveSession(session);
                    session.removeParticipant(affectedUser);
                }

                UserService.sessionChanged(session);

            } else {
                LOG.warn("Unknown message type [{}] in message {} from {}", type, message.getPayload(), channel);
            }
        } catch (Exception e) {
            LOG.error("Failed to handle message {} from {}", message.getPayload(), channel, e);
        }
    }

    private void unregisterChannel(WebSocketSession channel) {
        this.allChannels.remove(channel);

        final SessionParticipant participant = this.claimedChannels.remove(channel);

        if (participant != null) {
            participant.removeWebSocketChannel(channel);

            if (!participant.isSuperuser()
                    && participant.getWebSocketNoSessionChannels().isEmpty() && participant.getWebSocketSessionChannels().isEmpty()
                    && sessionService.findByParticipant(participant).isEmpty()
            ) {
                userService.unregisterParticipant(participant);
            }
        }
    }
}
