package com.clinicflow.web.clinic;

import com.clinicflow.audit.AuditService;
import com.clinicflow.domain.entity.Clinic;
import com.clinicflow.domain.entity.ClinicPatient;
import com.clinicflow.domain.entity.Patient;
import com.clinicflow.domain.entity.PatientLinkRequest;
import com.clinicflow.domain.entity.User;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.LinkRequestMethod;
import com.clinicflow.domain.enums.LinkRequestStatus;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.repository.AppointmentRepository;
import com.clinicflow.domain.repository.ClinicPatientRepository;
import com.clinicflow.domain.repository.PatientLinkRequestRepository;
import com.clinicflow.domain.repository.PatientRepository;
import com.clinicflow.domain.repository.PrescriptionRepository;
import com.clinicflow.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicLinkRequestServiceTest {

    @Mock
    private PatientLinkRequestRepository linkRequestRepository;

    @Mock
    private ClinicPatientRepository clinicPatientRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ClinicLinkRequestService service;

    @Test
    void approveCreatesClinicLinkAndAudits() {
        Clinic clinic = clinic(2L, "Northside Family Clinic");
        Patient patient = patient(7L, "PAT-2026-0007", "Riya", "Sharma");
        PatientLinkRequest request = request(5L, clinic, patient);
        User actor = user(42L, "northadmin@clinicflow.local");

        when(linkRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(clinicPatientRepository.existsByClinicClinicIdAndPatientPatientId(2L, 7L)).thenReturn(false);
        when(patientRepository.findByPublicPatientId(patient.getPublicPatientId())).thenReturn(Optional.of(patient));
        when(userRepository.findById(42L)).thenReturn(Optional.of(actor));

        ClinicLinkRequestService.DecisionResult result =
                service.approve(2L, 5L, 42L, patient.getPublicPatientId(), "Verified at the front desk.");

        assertThat(result.success()).isTrue();
        assertThat(request.getStatus()).isEqualTo(LinkRequestStatus.APPROVED);
        assertThat(request.getClinicPatientRecord()).isSameAs(patient);
        assertThat(request.getResolvedBy()).isSameAs(actor);
        assertThat(request.getResolvedAt()).isNotNull();
        assertThat(request.getResolutionNote()).isEqualTo("Verified at the front desk.");

        ArgumentCaptor<ClinicPatient> captor = ArgumentCaptor.forClass(ClinicPatient.class);
        verify(clinicPatientRepository).save(captor.capture());
        assertThat(captor.getValue().getClinic()).isSameAs(clinic);
        assertThat(captor.getValue().getPatient()).isSameAs(patient);
        verify(linkRequestRepository).save(request);
        verify(auditService).record(eq(42L), eq(Role.CLINIC_ADMIN), eq("PATIENT_LINK_APPROVED"),
                eq("PatientLinkRequest"), eq("5"), eq(7L), eq(AuditOutcome.SUCCESS));
    }

    @Test
    void approveRejectsUnlinkedClinicPatientRecord() {
        Clinic clinic = clinic(2L, "Northside Family Clinic");
        Patient accountPatient = patient(7L, "PAT-2026-0007", "Riya", "Sharma");
        Patient otherRecord = patient(8L, "PAT-2026-0008", "Riya", "Sharma");
        PatientLinkRequest request = request(5L, clinic, accountPatient);

        when(linkRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(patientRepository.findByPublicPatientId("PAT-2026-0008")).thenReturn(Optional.of(otherRecord));
        when(clinicPatientRepository.existsByClinicClinicIdAndPatientPatientId(2L, 8L)).thenReturn(false);

        ClinicLinkRequestService.DecisionResult result =
                service.approve(2L, 5L, 42L, "PAT-2026-0008", null);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("not linked to your clinic");
        assertThat(request.getStatus()).isEqualTo(LinkRequestStatus.PENDING);
        verify(clinicPatientRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(auditService, never()).record(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void approveMergesAccountIntoExistingClinicRecord() {
        Clinic clinic = clinic(2L, "Northside Family Clinic");
        Patient account = patient(7L, "PAT-2026-0007", "Riya", "Sharma");
        User accountUser = user(11L, "patient@clinicflow.local");
        account.setUser(accountUser);
        account.setAddressLine1("44 Lake View Road");
        account.setAllergies("Penicillin");
        Patient clinicRecord = patient(8L, "PAT-2026-0008", "Riya", "Sharma");
        PatientLinkRequest request = request(5L, clinic, account);
        User actor = user(42L, "northadmin@clinicflow.local");

        when(linkRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(patientRepository.findByPublicPatientId("PAT-2026-0008")).thenReturn(Optional.of(clinicRecord));
        when(clinicPatientRepository.existsByClinicClinicIdAndPatientPatientId(2L, 8L)).thenReturn(true);
        when(clinicPatientRepository.findByPatientPatientId(7L)).thenReturn(List.of());
        when(appointmentRepository.findByPatientPatientIdOrderBySlotStartDesc(7L)).thenReturn(List.of());
        when(prescriptionRepository.findByPatientPatientIdOrderByIssueDateDesc(7L)).thenReturn(List.of());
        when(linkRequestRepository.findByPatientPatientIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(request));
        when(userRepository.findById(42L)).thenReturn(Optional.of(actor));

        ClinicLinkRequestService.DecisionResult result =
                service.approve(2L, 5L, 42L, "PAT-2026-0008", "Merged duplicate.");

        assertThat(result.success()).isTrue();
        assertThat(clinicRecord.getUser()).isSameAs(accountUser);
        assertThat(account.getUser()).isNull();
        assertThat(clinicRecord.getEmail()).isEqualTo("patient@clinicflow.local");
        assertThat(clinicRecord.getAddressLine1()).isEqualTo("44 Lake View Road");
        assertThat(clinicRecord.getAllergies()).isEqualTo("Penicillin");
        assertThat(request.getPatient()).isSameAs(clinicRecord);
        assertThat(request.getClinicPatientRecord()).isSameAs(clinicRecord);
        assertThat(request.getStatus()).isEqualTo(LinkRequestStatus.APPROVED);
        assertThat(request.getResolutionNote()).isEqualTo("Merged duplicate.");
        verify(patientRepository).save(clinicRecord);
        verify(patientRepository).save(account);
        verify(auditService).record(eq(42L), eq(Role.CLINIC_ADMIN), eq("PATIENT_RECORD_MERGED"),
                eq("Patient"), eq("PAT-2026-0007"), eq(8L), eq(AuditOutcome.SUCCESS));
        verify(auditService).record(eq(42L), eq(Role.CLINIC_ADMIN), eq("PATIENT_LINK_APPROVED"),
                eq("PatientLinkRequest"), eq("5"), eq(8L), eq(AuditOutcome.SUCCESS));
    }

    @Test
    void approveRejectsRequestFromAnotherClinic() {
        Clinic clinic = clinic(2L, "Northside Family Clinic");
        Patient patient = patient(7L, "PAT-2026-0007", "Riya", "Sharma");
        PatientLinkRequest request = request(5L, clinic, patient);

        when(linkRequestRepository.findById(5L)).thenReturn(Optional.of(request));

        ClinicLinkRequestService.DecisionResult result = service.approve(1L, 5L, 42L, null, null);

        assertThat(result.success()).isFalse();
        assertThat(request.getStatus()).isEqualTo(LinkRequestStatus.PENDING);
        verify(clinicPatientRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectMarksRequestResolvedAndAudits() {
        Clinic clinic = clinic(2L, "Northside Family Clinic");
        Patient patient = patient(7L, "PAT-2026-0007", "Riya", "Sharma");
        PatientLinkRequest request = request(5L, clinic, patient);
        User actor = user(42L, "northadmin@clinicflow.local");

        when(linkRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(userRepository.findById(42L)).thenReturn(Optional.of(actor));

        ClinicLinkRequestService.DecisionResult result =
                service.reject(2L, 5L, 42L, "Could not verify the phone number.");

        assertThat(result.success()).isTrue();
        assertThat(request.getStatus()).isEqualTo(LinkRequestStatus.REJECTED);
        assertThat(request.getResolvedBy()).isSameAs(actor);
        assertThat(request.getResolvedAt()).isNotNull();
        assertThat(request.getResolutionNote()).isEqualTo("Could not verify the phone number.");
        verify(linkRequestRepository).save(request);
        verify(auditService).record(eq(42L), eq(Role.CLINIC_ADMIN), eq("PATIENT_LINK_REJECTED"),
                eq("PatientLinkRequest"), eq("5"), eq(7L), eq(AuditOutcome.SUCCESS));
    }

    private PatientLinkRequest request(Long id, Clinic clinic, Patient patient) {
        PatientLinkRequest request = new PatientLinkRequest();
        request.setLinkRequestId(id);
        request.setClinic(clinic);
        request.setPatient(patient);
        request.setMethod(LinkRequestMethod.CLINIC_APPROVAL);
        request.setStatus(LinkRequestStatus.PENDING);
        request.setAttempts(0);
        request.setCreatedAt(LocalDateTime.now());
        return request;
    }

    private Patient patient(Long id, String publicId, String firstName, String lastName) {
        Patient patient = new Patient();
        patient.setPatientId(id);
        patient.setPublicPatientId(publicId);
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setDob(LocalDate.of(1994, 4, 18));
        return patient;
    }

    private Clinic clinic(Long id, String name) {
        Clinic clinic = new Clinic();
        clinic.setClinicId(id);
        clinic.setName(name);
        return clinic;
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setUserId(id);
        user.setEmail(email);
        return user;
    }
}
