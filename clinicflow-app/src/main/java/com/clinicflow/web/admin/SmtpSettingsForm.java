package com.clinicflow.web.admin;

import com.clinicflow.domain.enums.SmtpSecurity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmtpSettingsForm {

    @NotBlank
    @Size(max = 200)
    private String host;

    @NotNull
    @Min(1)
    @Max(65535)
    private Integer port = 587;

    @NotNull
    private SmtpSecurity security = SmtpSecurity.STARTTLS;

    @Size(max = 200)
    private String username;

    @Size(max = 200)
    private String password;

    @Size(max = 200)
    private String fromName;

    @Email
    @Size(max = 254)
    private String fromAddress;

    private boolean active = true;

    @Email
    @Size(max = 254)
    private String testRecipient;
}
