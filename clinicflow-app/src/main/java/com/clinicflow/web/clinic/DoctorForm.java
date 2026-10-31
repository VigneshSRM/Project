package com.clinicflow.web.clinic;

import com.clinicflow.domain.enums.DoctorStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DoctorForm {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 100)
    private String specialization;

    @DecimalMin(value = "0.00")
    private BigDecimal fee;

    @NotNull
    private DoctorStatus status = DoctorStatus.ACTIVE;

    @Email
    @Size(max = 254)
    private String email;
}
