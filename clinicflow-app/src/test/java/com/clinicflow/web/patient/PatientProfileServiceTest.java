package com.clinicflow.web.patient;

import com.clinicflow.audit.AuditService;
import com.clinicflow.domain.entity.Patient;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.Gender;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.repository.ClinicPatientRepository;
import com.clinicflow.domain.repository.PatientLinkRequestRepository;
import com.clinicflow.domain.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientProfileServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ClinicPatientRepository clinicPatientRepository;

    @Mock
    private PatientLinkRequestRepository linkRequestRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PatientProfileService service;

    @Test
    void updateScopesToAuthenticatedUsersPatientAndAudits() {
        Patient patient = new Patient();
        patient.setPatientId(7L);
        patient.setPublicPatientId("PAT-2026-0007");
        when(patientRepository.findByUserUserId(42L)).thenReturn(Optional.of(patient));

        PatientProfileForm form = new PatientProfileForm();
        form.setFirstName("  Riya ");
        form.setLastName(" Sharma  ");
        form.setGender(Gender.FEMALE);
        form.setAddressLine1("  14 Updated Street  ");
        form.setCity(" Pune ");
        form.setBloodGroup("B+");
        form.setAllergies(" Penicillin ");

        assertThat(service.update(42L, form)).isTrue();

        assertThat(patient.getFirstName()).isEqualTo("Riya");
        assertThat(patient.getLastName()).isEqualTo("Sharma");
        assertThat(patient.getAddressLine1()).isEqualTo("14 Updated Street");
        assertThat(patient.getCity()).isEqualTo("Pune");
        assertThat(patient.getAllergies()).isEqualTo("Penicillin");
        assertThat(patient.getGender()).isEqualTo(Gender.FEMALE);
        verify(patientRepository).save(patient);
        verify(auditService).record(eq(42L), eq(Role.PATIENT), eq("PATIENT_PROFILE_UPDATED"),
                eq("Patient"), eq("PAT-2026-0007"), eq(7L), eq(AuditOutcome.SUCCESS));
        verifyNoInteractions(clinicPatientRepository, linkRequestRepository);
    }

    @Test
    void updateReturnsFalseWhenNoPatientIsLinkedAndDoesNotAudit() {
        when(patientRepository.findByUserUserId(42L)).thenReturn(Optional.empty());

        assertThat(service.update(42L, new PatientProfileForm())).isFalse();

        verifyNoInteractions(auditService, clinicPatientRepository, linkRequestRepository);
    }
}
