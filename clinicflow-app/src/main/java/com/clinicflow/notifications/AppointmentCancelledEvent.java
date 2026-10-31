package com.clinicflow.notifications;

public record AppointmentCancelledEvent(Long appointmentId, String patientEmail, String patientName,
                                        String clinicName, String doctorName, String when,
                                        String appointmentPublicId, String reason) {
}
