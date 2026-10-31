package com.clinicflow.web.clinic;

import java.util.List;

/**
 * Read model for the clinic-side link request queue. It intentionally carries only
 * the identity fields needed to verify a patient before approval.
 */
public record ClinicLinkRequestListView(List<Row> requests, long pendingCount, long totalCount) {

    public record Row(Long id, String patientName, String publicPatientId, String mobile,
                      String requestedOn, String method, String status, String statusTone) {
    }
}
