package de.yanwittmann.presentation.model.out;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
public class OutFullSession {
    private final UUID id;
    private final Date creationDate;
    private final String name;
    private final String password;
    private final List<OutSelfSessionParticipant> participants;
    private final Date timerTargetDate;
}
