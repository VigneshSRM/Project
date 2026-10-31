package com.clinicflow.web.patient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailChangeForm {

    @NotBlank(message = "Enter the new email address.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 254)
    private String newEmail;

    @NotBlank(message = "Enter your current password.")
    private String currentPassword;
}
