package com.clinicflow.notifications;

public record AppointmentBookedEvent(Long appointmentId, String patientEmail, String patientName,
                                     String clinicName, String doctorName, String when, String reason,
                                     String appointmentPublicId) {
}
