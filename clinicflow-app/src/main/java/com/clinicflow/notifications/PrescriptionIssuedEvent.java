package com.clinicflow.notifications;

public record PrescriptionIssuedEvent(Long prescriptionId, String patientEmail, String patientName,
                                      String prescriptionCode, String clinicName, String doctorName,
                                      String issueDate, String expiryDate, String suffix) {
}
