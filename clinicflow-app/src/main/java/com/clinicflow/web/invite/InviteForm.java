package com.clinicflow.web.invite;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteForm {

    @NotBlank
    private String token;

    @NotBlank(message = "Choose a password.")
    private String password;

    @NotBlank(message = "Confirm the password.")
    private String confirmPassword;
}
