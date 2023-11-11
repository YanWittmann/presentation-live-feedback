package de.yanwittmann.presentation.model.internal;

import de.yanwittmann.presentation.model.out.OutOtherSessionParticipant;
import de.yanwittmann.presentation.model.out.OutSelfSessionParticipant;
import de.yanwittmann.presentation.service.UserService;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class SessionParticipant {

    private final static Logger LOG = LogManager.getLogger(SessionParticipant.class);

    private final UUID id = UUID.randomUUID();
    private final UUID referenceId = UUID.randomUUID();
    private final String username;
    private final String uniqueComputerId;
    private final Color color;

    private final Map<Session, SessionState> sessionStates = new ConcurrentHashMap<>();

    @Setter
    @Getter
    private boolean isSuperuser = false;

    private final Map<Session, Set<WebSocketSession>> webSocketSessionChannels = new HashMap<>();
    private final Set<WebSocketSession> webSocketNoSessionChannels = new HashSet<>();

    public SessionParticipant(String username, String uniqueComputerId) {
        this.username = username;
        this.uniqueComputerId = uniqueComputerId;
        final float hue = (float) (Math.abs(uniqueComputerId.hashCode() + username.hashCode()) % 360) / 360f;
        this.color = Color.getHSBColor(hue, 0.5f, 0.7f);
    }

    public Session findSessionByChannel(WebSocketSession channel) {
        for (Map.Entry<Session, Set<WebSocketSession>> sessionWebSocketEntry : webSocketSessionChannels.entrySet()) {
            if (sessionWebSocketEntry.getValue().contains(channel)) {
                return sessionWebSocketEntry.getKey();
            }
        }
        return null;
    }

    public void enterSession(Session session, WebSocketSession channel) {
        synchronized (webSocketSessionChannels) {
            webSocketSessionChannels.computeIfAbsent(session, s -> new HashSet<>()).add(channel);
        }
        sessionStates.computeIfAbsent(session, s -> new SessionState());
        lastMessageTimes.clear();
        LOG.info("Registered channel [{}] on user {} for session {}", channel, this, session);
    }

    public void leaveSession(Session session, WebSocketSession channel) {
        synchronized (webSocketSessionChannels) {
            webSocketSessionChannels.computeIfAbsent(session, s -> new HashSet<>()).remove(channel);
        }
        sessionStates.remove(session);
        lastMessageTimes.clear();
        LOG.info("Participant {} left session {} with channel [{}]", this, session, channel);
    }

    public void leaveSession(Session session) {
        synchronized (webSocketSessionChannels) {
            webSocketSessionChannels.remove(session);
        }
        sessionStates.remove(session);
        lastMessageTimes.clear();
        LOG.info("Participant {} left session {}", this, session);
    }

    public void enterNoSession(WebSocketSession webSocketSession) {
        synchronized (webSocketNoSessionChannels) {
            webSocketNoSessionChannels.add(webSocketSession);
        }
        lastMessageTimes.clear();
        LOG.info("Participant {} entered no session", this);
    }

    public void leaveNoSession(WebSocketSession webSocketSession) {
        synchronized (webSocketNoSessionChannels) {
            webSocketNoSessionChannels.remove(webSocketSession);
        }
        lastMessageTimes.clear();
        LOG.info("Participant {} left no session", this);
    }

    private final Map<String, Long> lastMessageTimes = Collections.synchronizedMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > 10;
        }
    });

    private final static long MESSAGE_DEBOUNCE_TIME = 10L; // ms

    private long getTimeSinceLastMessage(String message) {
        return System.currentTimeMillis() - lastMessageTimes.getOrDefault(message, 0L);
    }

    private boolean debounceWsMessageOnChannel(JSONObject message, String jsonMessage, WebSocketSession channel) {
        final String timeLookupKey = channel.getId() + jsonMessage;
        final long timeSinceLastMessage = getTimeSinceLastMessage(timeLookupKey);
        if (timeSinceLastMessage < MESSAGE_DEBOUNCE_TIME) {
            LOG.info("[SKIP DEBOUNCE] Skipping message [{}] on user [{}] to no channels due to debounce time of {}ms ({}ms since last message)", message, this, MESSAGE_DEBOUNCE_TIME, timeSinceLastMessage);
            return true;
        }
        lastMessageTimes.put(timeLookupKey, System.currentTimeMillis());
        return false;
    }

    public void sendToWebSocketChannelsBySession(Session session, JSONObject message) {
        LOG.info("[WS REQUEST   ] On user {} to session {}", this, session);

        final HashSet<WebSocketSession> channels;
        synchronized (webSocketSessionChannels) {
            channels = new HashSet<>(webSocketSessionChannels.getOrDefault(session, Set.of()));
        }

        if (channels.isEmpty()) {
            LOG.info("[SKIP NO CHA  ] Skipping message [{}] on user [{}] to session channels due to no session channels", message, this);
            return;
        }

        final String jsonMessage = message.toString();
        for (WebSocketSession channel : channels) {
            if (channel.isOpen()) {
                try {
                    if (debounceWsMessageOnChannel(message, jsonMessage, channel)) continue;

                    if (jsonMessage.contains("move-to-session")) {
                        System.out.println("MOVE TO SESSION: " + jsonMessage);
                    }

                    LOG.info("[SEND         ] Sending message [{}] on user [{}] and session [{}] to channel [{}]", message, this, session, channel);
                    channel.sendMessage(new TextMessage(jsonMessage));
                } catch (Exception e) {
                    LOG.error("Failed to send message [{}] to session {}", message, channel, e);
                }
            }
        }
    }

    public void sendToWebSocketChannelsNoSession(JSONObject message) {
        LOG.info("[WS REQUEST NO] On user {}", this);

        final HashSet<WebSocketSession> channels;
        synchronized (webSocketNoSessionChannels) {
            channels = new HashSet<>(webSocketNoSessionChannels);
        }

        if (channels.isEmpty()) {
            return;
        }

        final String jsonMessage = message.toString();
        for (WebSocketSession channel : channels) {
            if (channel.isOpen()) {
                try {
                    if (debounceWsMessageOnChannel(message, jsonMessage, channel)) continue;

                    LOG.info("[SEND         ] Sending message [{}] on user [{}] to channel [{}]", message, this, channel);
                    channel.sendMessage(new TextMessage(message.toString()));
                } catch (Exception e) {
                    LOG.error("Failed to send message [{}] to session {}", message, channel, e);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "{" + username + " : " + id + "}";
    }

    public SessionState getSessionState(Session session) {
        return sessionStates.computeIfAbsent(session, s -> new SessionState());
    }

    public OutSelfSessionParticipant toOutSelfSessionParticipant() {
        return new OutSelfSessionParticipant(id, referenceId, username, color);
    }

    public OutOtherSessionParticipant toOutOtherSessionParticipant(Session session) {
        return new OutOtherSessionParticipant(referenceId, username, color, isSuperuser, getSessionState(session).toJson().toString());
    }

    public void notifySessionChanged(Session session, List<OutOtherSessionParticipant> outParticipants) {
        this.sendToWebSocketChannelsNoSession(new JSONObject()
                .put("type", "session-changed")
                .put("participants", outParticipants)
        );
        this.sendToWebSocketChannelsBySession(session, new JSONObject()
                .put("type", "session-changed")
                .put("participants", outParticipants)
        );
    }

    public void removeWebSocketChannel(WebSocketSession channel) {
        webSocketNoSessionChannels.remove(channel);
        for (Map.Entry<Session, Set<WebSocketSession>> sessionWebSocketEntry : webSocketSessionChannels.entrySet()) {
            final Set<WebSocketSession> webSocketSessions = sessionWebSocketEntry.getValue();
            final Session session = sessionWebSocketEntry.getKey();
            webSocketSessions.remove(channel);
            if (webSocketSessions.isEmpty()) {
                session.removeParticipant(this);
                session.notifySessionChanged();
                UserService.sessionChanged(session);
            }
        }
    }
}
