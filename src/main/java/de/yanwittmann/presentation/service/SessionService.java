package de.yanwittmann.presentation.service;

import de.yanwittmann.presentation.model.internal.Session;
import de.yanwittmann.presentation.model.internal.SessionParticipant;
import de.yanwittmann.presentation.model.out.OutFullSession;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Service
public class SessionService {

    private final static Logger LOG = LogManager.getLogger(SessionService.class);

    private final List<Session> sessions = new ArrayList<>();

    public Session createSession(String name, String password) {
        if (findSessionByName(name) != null) {
            throw new RuntimeException("Session name already taken");
        }
        final Session session = new Session(name, password);
        sessions.add(session);
        LOG.info("Created session [{}] with{} password protection",
                session.getName(), session.getPassword() == null ? "out" : "");
        return session;
    }

    public Session findSessionByName(String name) {
        return sessions.stream()
                .filter(session -> session.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public List<OutFullSession> getOutSessions() {
        return sessions.stream().map(Session::toOutFullSession).collect(Collectors.toList());
    }

    public Session findSessionById(UUID uuid) {
        return sessions.stream()
                .filter(session -> session.getId().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    public void close(Session session) {
        session.close();
        sessions.remove(session);
    }

    public List<Session> findByParticipant(SessionParticipant participant) {
        return sessions.stream()
                .filter(session -> session.getParticipants().contains(participant))
                .collect(Collectors.toList());
    }
}
