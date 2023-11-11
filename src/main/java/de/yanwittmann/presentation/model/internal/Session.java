package de.yanwittmann.presentation.model.internal;

import de.yanwittmann.presentation.model.out.OutFullSession;
import de.yanwittmann.presentation.model.out.OutOtherSessionParticipant;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class Session {

    private final UUID id = UUID.randomUUID();
    private final Date creationDate = new Date();
    private final String name;
    private final String password;
    private final Set<SessionParticipant> participants = new HashSet<>();

    @Setter
    private Date timerTargetDate;

    public Session(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public void addParticipant(SessionParticipant participant) {
        participants.add(participant);
        notifySessionChanged();
    }

    public void removeParticipant(SessionParticipant participant) {
        participants.remove(participant);
        notifySessionChanged();
    }

    public void assertPasswordCorrect(String sessionPassword) {
        if (password != null && !password.equals(sessionPassword)) {
            throw new RuntimeException("Invalid session password");
        }
    }

    public OutFullSession toOutFullSession() {
        return new OutFullSession(id, creationDate, name, password, participants.stream().map(SessionParticipant::toOutSelfSessionParticipant).collect(Collectors.toList()), timerTargetDate);
    }

    public List<OutOtherSessionParticipant> toOutOtherParticipants() {
        return participants.stream().map(p -> p.toOutOtherSessionParticipant(this)).collect(Collectors.toList());
    }

    public void close() {
        List<SessionParticipant> participantsCopy = new ArrayList<>(participants);
        participants.clear();
        for (SessionParticipant participant : participantsCopy) {
            participant.sendToWebSocketChannelsBySession(this, new JSONObject()
                    .put("type", "session-closed")
            );
            participant.leaveSession(this);
            this.removeParticipant(participant);
        }
        notifySessionChanged();
    }

    public void notifySessionChanged() {
        final List<OutOtherSessionParticipant> outParticipants = this.toOutOtherParticipants();
        for (SessionParticipant participant : participants) {
            participant.notifySessionChanged(this, outParticipants);
        }
    }

    @Override
    public String toString() {
        return "{" + name + " : " + id + "}";
    }
}
