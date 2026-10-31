package com.clinicflow.web.patient;

import com.clinicflow.domain.entity.Patient;

import java.util.List;

/**
 * Read model for the profile page. Clinic names are resolved inside the service
 * transaction so the template never touches a lazy association with open-in-view
 * disabled.
 */
public record PatientProfileView(Patient patient,
                                 List<LinkedClinicView> linkedClinics,
                                 List<LinkRequestView> linkRequests) {

    public record LinkedClinicView(String clinicName, String city, String linkedOn) {
    }

    public record LinkRequestView(String clinicName, String method, String status,
                                  String statusTone, String requestedOn,
                                  String resolvedOn, String resolutionNote) {
    }
}
