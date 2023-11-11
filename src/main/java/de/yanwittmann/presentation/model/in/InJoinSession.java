package de.yanwittmann.presentation.model.in;

import lombok.Data;

@Data
public class InJoinSession {
    private String username;
    private String uniqueComputerId;
    private String sessionName;
    private String sessionPassword;
    private boolean spectator;
}
