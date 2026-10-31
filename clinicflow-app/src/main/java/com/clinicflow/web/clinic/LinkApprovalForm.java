package com.clinicflow.web.clinic;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Approving a link is an identity-verification decision, not a data-entry shortcut.
 * The checkbox is deliberately required so an accidental click cannot link records.
 */
@Getter
@Setter
public class LinkApprovalForm {

    @Size(max = 20, message = "Clinic patient ID is too long.")
    private String clinicPatientPublicId;

    @AssertTrue(message = "Confirm that you verified the patient's identity.")
    private boolean identityVerified;

    @Size(max = 500, message = "Note is too long.")
    private String note;
}
