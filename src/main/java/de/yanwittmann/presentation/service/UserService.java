package de.yanwittmann.presentation.service;

import de.yanwittmann.presentation.PresentationFeedbackEntrypoint;
import de.yanwittmann.presentation.model.internal.Session;
import de.yanwittmann.presentation.model.internal.SessionParticipant;
import de.yanwittmann.presentation.model.out.OutOtherSessionParticipant;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;

@Getter
@Service
public class UserService {

    private final static Logger LOG = LogManager.getLogger(UserService.class);

    private final static List<Consumer<Session>> SESSION_CHANGED_LISTENERS = Collections.synchronizedList(new ArrayList<>());

    public static void addSessionChangedListener(Consumer<Session> listener) {
        synchronized (SESSION_CHANGED_LISTENERS) {
            SESSION_CHANGED_LISTENERS.add(listener);
        }
    }

    public static void sessionChanged(Session session) {
        synchronized (SESSION_CHANGED_LISTENERS) {
            SESSION_CHANGED_LISTENERS.forEach(listener -> listener.accept(session));
        }
    }

    private final List<SessionParticipant> participants = new ArrayList<>();

    public UserService() {
        addSessionChangedListener(this::notifyAllSessionManagersOfSessionChanged);
    }

    public SessionParticipant registerParticipant(SessionParticipant participant) {
        participants.add(participant);
        LOG.info("Registered participant [{}]", participant);
        return participant;
    }

    public void unregisterParticipant(SessionParticipant participant) {
        participants.remove(participant);
        LOG.info("Unregistered participant [{}]", participant);
    }

    public void assertSuperuser(String authToken) {
        if (!PresentationFeedbackEntrypoint.getPasswordValue().equals(authToken)) {
            throw new RuntimeException("Invalid superuser token");
        }
    }

    public SessionParticipant findUserByNameAndComputerId(String name, String computerId) {
        return participants.stream()
                .filter(participant -> participant.getUsername().equals(name) && participant.getUniqueComputerId().equals(computerId))
                .findFirst()
                .orElse(null);
    }

    public SessionParticipant findUserById(UUID uuid) {
        return participants.stream()
                .filter(participant -> participant.getId().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    public SessionParticipant findUserByReferenceId(UUID uuid) {
        return participants.stream()
                .filter(participant -> participant.getReferenceId().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    public void notifyAllSessionManagersOfSessionChanged(Session session) {
        final List<OutOtherSessionParticipant> outParticipants = session.toOutOtherParticipants();
        for (SessionParticipant participant : participants) {
            participant.notifySessionChanged(session, outParticipants);
        }
    }
}
