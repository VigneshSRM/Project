package com.clinicflow.web.setup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetupForm {

    @NotBlank(message = "Enter the platform administrator email.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 254)
    private String email;

    @NotBlank(message = "Choose a password.")
    private String password;

    @NotBlank(message = "Confirm the password.")
    private String confirmPassword;
}
