package com.clinicflow.web.patient;

/**
 * A clinic the patient can ask to link to. Only active clinics without an existing
 * link or a pending request are offered.
 */
public record ClinicOption(Long clinicId, String name, String city) {
}
