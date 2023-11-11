package de.yanwittmann.presentation.model.out;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.awt.*;
import java.util.UUID;

@Data
@AllArgsConstructor
public class OutSelfSessionParticipant {
    private UUID userId;
    private UUID referenceUserId;
    private String username;
    private Color userColor;
}
