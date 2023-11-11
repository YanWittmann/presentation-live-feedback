package de.yanwittmann.presentation.model.in;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class InManagerLogin extends InAuthorization {
    private String username;
    private String uniqueComputerId;
}
