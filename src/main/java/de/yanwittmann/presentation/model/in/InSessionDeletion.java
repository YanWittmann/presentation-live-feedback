package de.yanwittmann.presentation.model.in;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class InSessionDeletion extends InUserId {
    private String sessionId;
}

