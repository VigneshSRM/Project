package com.clinicflow.web.clinic;

import java.util.List;

/**
 * Detail view for one account-to-clinic link request. Duplicate matches are other
 * clinic patient records with the same name and date of birth; they are shown as a
 * warning so staff do not create an accidental duplicate clinical identity.
 */
public record ClinicLinkRequestDetailView(
        Long id,
        String clinicName,
        String patientName,
        String publicPatientId,
        String dob,
        String gender,
        String mobile,
        String email,
        String addressLine1,
        String addressLine2,
        String city,
        String district,
        String state,
        String postalCode,
        String method,
        String status,
        String statusTone,
        String requestedOn,
        String resolvedOn,
        String resolvedBy,
        String resolutionNote,
        boolean canDecide,
        List<DuplicateMatch> duplicates) {

    public record DuplicateMatch(String publicPatientId, String name, String dob,
                                 String mobile, String linkedOn) {
    }
}
