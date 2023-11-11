package de.yanwittmann.presentation.model.out;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.awt.*;
import java.util.UUID;

@Data
@AllArgsConstructor
public class OutOtherSessionParticipant {
    private UUID referenceUserId;
    private String username;
    private String userColor;
    private String sessionState;
    private String message;
    private boolean isSuperuser;

    public OutOtherSessionParticipant(UUID referenceUserId, String username, Color userColor, boolean isSuperuser, String sessionState) {
        this.referenceUserId = referenceUserId;
        this.username = username;
        this.userColor = "#" + Integer.toHexString(userColor.getRGB()).substring(2);
        this.isSuperuser = isSuperuser;
        this.sessionState = sessionState;
    }

    public OutOtherSessionParticipant(UUID referenceUserId, String username, Color userColor, boolean isSuperuser) {
        this.referenceUserId = referenceUserId;
        this.username = username;
        this.userColor = "#" + Integer.toHexString(userColor.getRGB()).substring(2);
        this.isSuperuser = isSuperuser;
    }

    public OutOtherSessionParticipant(String message) {
        this.message = message;
    }
}
