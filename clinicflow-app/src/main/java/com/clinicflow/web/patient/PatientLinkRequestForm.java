package com.clinicflow.web.patient;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Patient-side request to connect the signed-in account to a clinic. The clinic
 * always verifies identity before the link is created.
 */
@Getter
@Setter
public class PatientLinkRequestForm {

    @NotNull(message = "Select a clinic.")
    private Long clinicId;
}
