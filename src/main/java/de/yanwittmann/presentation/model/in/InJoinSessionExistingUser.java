package de.yanwittmann.presentation.model.in;

import lombok.Data;

@Data
public class InJoinSessionExistingUser {
    private String userId;
    private String sessionName;
    private String sessionPassword;
    private boolean spectator;
}
