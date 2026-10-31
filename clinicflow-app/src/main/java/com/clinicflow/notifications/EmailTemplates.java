package com.clinicflow.notifications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Plain-text, minimum-disclosure templates. The verification/reset emails contain
 * only the recipient's first name and a single-use link.
 */
@Service
public class EmailTemplates {

    private final String baseUrl;
    private final long verifyEmailTtlHours;
    private final long resetPasswordTtlMinutes;

    public EmailTemplates(@Value("${clinicflow.base-url:http://localhost:8080}") String baseUrl,
                          @Value("${clinicflow.auth.verify-email-ttl-hours:24}") long verifyEmailTtlHours,
                          @Value("${clinicflow.auth.reset-password-ttl-minutes:60}") long resetPasswordTtlMinutes) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.verifyEmailTtlHours = verifyEmailTtlHours;
        this.resetPasswordTtlMinutes = resetPasswordTtlMinutes;
    }

    public EmailContent verification(String firstName, String rawToken) {
        String link = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/verify-email")
                .queryParam("token", rawToken)
                .build()
                .toUriString();
        String body = """
                Hello %s,

                Welcome to IMS. Confirm your email address to activate your patient account:

                %s

                This link expires in %d hours and can be used once.
                If you did not create this account, you can ignore this email.

                IMS
                """.formatted(oneLine(firstName), link, verifyEmailTtlHours);
        return new EmailContent("Verify your IMS email", body);
    }

    public EmailContent staffInvite(String name, String organization, String roleLabel, String rawToken) {
        String link = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/invite")
                .queryParam("token", rawToken)
                .build()
                .toUriString();
        String body = """
                Hello,

                %s invited you to IMS as %s.

                Choose your password and activate the account:
                %s

                This link expires in 72 hours and can be used once.
                If you were not expecting this invitation, ignore this email.

                IMS
                """.formatted(oneLine(organization), oneLine(roleLabel), link);
        return new EmailContent("Your IMS invitation", body);
    }

    public EmailContent emailChange(String firstName, String newEmail, String rawToken) {
        String link = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/verify-email-change")
                .queryParam("token", rawToken)
                .build()
                .toUriString();
        String body = """
                Hello %s,

                Confirm this address as the new sign-in email for your IMS account:
                %s

                The link expires in 24 hours. Your current email stays active until it is confirmed.
                If you did not request this change, ignore this email and contact your clinic.

                IMS
                """.formatted(oneLine(firstName), link);
        return new EmailContent("Confirm your new IMS email", body);
    }

    public EmailContent appointmentConfirmation(String patientName, String clinicName, String doctorName,
                                                String when, String reason, String appointmentId) {
        String body = """
                Hello %s,

                Your appointment is booked.

                Appointment: %s
                Clinic: %s
                Doctor: %s
                When: %s
                Reason: %s

                You can view or cancel it from Appointments in your IMS account.

                IMS
                """.formatted(oneLine(patientName), appointmentId, oneLine(clinicName),
                oneLine(doctorName), oneLine(when), oneLine(reason));
        return new EmailContent("Appointment confirmed - " + appointmentId, body);
    }

    public EmailContent appointmentCancelled(String patientName, String clinicName, String doctorName,
                                             String when, String appointmentId, String reason) {
        String body = """
                Hello %s,

                Appointment %s at %s with %s on %s has been cancelled.

                Reason: %s

                You can book another appointment from Find Nearby Clinics.

                IMS
                """.formatted(oneLine(patientName), appointmentId, oneLine(clinicName),
                oneLine(doctorName), oneLine(when), oneLine(reason));
        return new EmailContent("Appointment cancelled - " + appointmentId, body);
    }

    public EmailContent prescriptionIssued(String patientName, String prescriptionCode, String clinicName,
                                           String doctorName, String issueDate, String expiryDate) {
        String body = """
                Hello %s,

                A prescription has been issued to you.

                Prescription ID: %s
                Clinic: %s
                Doctor: %s
                Issued: %s
                Valid until: %s

                Give the Prescription ID to the pharmacy. It is case-sensitive.

                IMS
                """.formatted(oneLine(patientName), prescriptionCode, oneLine(clinicName),
                oneLine(doctorName), oneLine(issueDate), oneLine(expiryDate));
        return new EmailContent("Prescription issued - " + prescriptionCode, body);
    }

    public EmailContent passwordReset(String firstName, String rawToken) {
        String link = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/reset-password")
                .queryParam("token", rawToken)
                .build()
                .toUriString();
        String body = """
                Hello %s,

                Use this single-use link to choose a new IMS password:

                %s

                This link expires in %d minutes. If you did not request a reset, ignore this email.

                IMS
                """.formatted(oneLine(firstName), link, resetPasswordTtlMinutes);
        return new EmailContent("Reset your IMS password", body);
    }

    private static String oneLine(String value) {
        if (value == null || value.isBlank()) {
            return "there";
        }
        return value.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8080";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
