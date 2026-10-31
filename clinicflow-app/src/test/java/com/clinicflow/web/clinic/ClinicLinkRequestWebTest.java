package com.clinicflow.web.clinic;

import com.clinicflow.domain.entity.Clinic;
import com.clinicflow.domain.entity.ClinicPatient;
import com.clinicflow.domain.entity.Patient;
import com.clinicflow.domain.entity.PatientLinkRequest;
import com.clinicflow.domain.entity.User;
import com.clinicflow.domain.enums.LinkRequestMethod;
import com.clinicflow.domain.enums.LinkRequestStatus;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.enums.UserStatus;
import com.clinicflow.domain.repository.ClinicPatientRepository;
import com.clinicflow.domain.repository.ClinicRepository;
import com.clinicflow.domain.repository.PatientLinkRequestRepository;
import com.clinicflow.domain.repository.PatientRepository;
import com.clinicflow.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class ClinicLinkRequestWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClinicRepository clinicRepository;

    @Autowired
    private ClinicPatientRepository clinicPatientRepository;

    @Autowired
    private PatientLinkRequestRepository linkRequestRepository;

    @Test
    void patientRequestsClinicAndClinicApproves() throws Exception {
        User patientUser = userRepository.findByEmailIgnoreCase("patient@clinicflow.local").orElseThrow();
        Patient patient = patientRepository.findByUserUserId(patientUser.getUserId()).orElseThrow();
        Clinic northside = northsideClinic();

        MockHttpSession patientSession = login("patient@clinicflow.local", "Patient@12345");
        mockMvc.perform(post("/patient/profile/link-requests").session(patientSession).with(csrf())
                        .param("clinicId", String.valueOf(northside.getClinicId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/profile"))
                .andExpect(flash().attributeExists("linkRequested"));

        mockMvc.perform(get("/patient/profile").session(patientSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Northside Family Clinic")))
                .andExpect(content().string(containsString("Pending")));

        PatientLinkRequest request = pendingRequest(northside.getClinicId(), patient.getPatientId());

        MockHttpSession clinicSession = login("northadmin@clinicflow.local", "North@12345");
        mockMvc.perform(get("/clinic/link-requests").session(clinicSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Riya Sharma")))
                .andExpect(content().string(containsString(
                        "/clinic/link-requests/" + request.getLinkRequestId())));

        mockMvc.perform(get("/clinic/link-requests/{id}", request.getLinkRequestId()).session(clinicSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Account identity")))
                .andExpect(content().string(containsString("Riya Sharma")))
                .andExpect(content().string(containsString("Northside Family Clinic")))
                .andExpect(content().string(containsString("Duplicate check")));

        mockMvc.perform(post("/clinic/link-requests/{id}/approve", request.getLinkRequestId())
                        .session(clinicSession).with(csrf())
                        .param("identityVerified", "true")
                        .param("clinicPatientPublicId", patient.getPublicPatientId())
                        .param("note", "Verified at the front desk."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clinic/link-requests/" + request.getLinkRequestId()))
                .andExpect(flash().attributeExists("approved"));

        mockMvc.perform(get("/clinic/link-requests/{id}", request.getLinkRequestId()).session(clinicSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Approved")))
                .andExpect(content().string(containsString("Verified at the front desk.")))
                .andExpect(content().string(containsString("Resolved")));

        MockHttpSession patientSessionAgain = login("patient@clinicflow.local", "Patient@12345");
        mockMvc.perform(get("/patient/profile").session(patientSessionAgain))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Northside Family Clinic")))
                .andExpect(content().string(containsString("Approved")));

        assertThat(clinicPatientRepository.existsByClinicClinicIdAndPatientPatientId(
                northside.getClinicId(), patient.getPatientId())).isTrue();
        PatientLinkRequest resolved = linkRequestRepository.findById(request.getLinkRequestId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(LinkRequestStatus.APPROVED);
        assertThat(resolved.getClinicPatientRecord()).isNotNull();
        assertThat(resolved.getClinicPatientRecord().getPatientId()).isEqualTo(patient.getPatientId());
    }

    @Test
    void clinicCannotOpenAnotherClinicsRequest() throws Exception {
        Patient patient = createPatient("PAT-TEST-ISO-1", "Iso", "Patient");
        PatientLinkRequest request = createPendingRequest(patient, northsideClinic());

        MockHttpSession cityCareSession = login("clinicadmin@clinicflow.local", "Clinic@12345");
        mockMvc.perform(get("/clinic/link-requests/{id}", request.getLinkRequestId()).session(cityCareSession))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/clinic/link-requests").session(cityCareSession))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(
                        "/clinic/link-requests/" + request.getLinkRequestId()))));

        MockHttpSession northsideSession = login("northadmin@clinicflow.local", "North@12345");
        mockMvc.perform(get("/clinic/link-requests/{id}", request.getLinkRequestId()).session(northsideSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Iso Patient")));
    }

    @Test
    void patientCannotOpenClinicQueue() throws Exception {
        MockHttpSession session = login("patient@clinicflow.local", "Patient@12345");
        mockMvc.perform(get("/clinic/link-requests").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void approvalRequiresIdentityVerification() throws Exception {
        Patient patient = createPatient("PAT-TEST-VERIFY-1", "Verify", "Patient");
        PatientLinkRequest request = createPendingRequest(patient, northsideClinic());

        MockHttpSession session = login("northadmin@clinicflow.local", "North@12345");
        mockMvc.perform(post("/clinic/link-requests/{id}/approve", request.getLinkRequestId())
                        .session(session).with(csrf())
                        .param("clinicPatientPublicId", patient.getPublicPatientId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Confirm that you verified the patient")));

        PatientLinkRequest unchanged = linkRequestRepository.findById(request.getLinkRequestId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(LinkRequestStatus.PENDING);
        assertThat(clinicPatientRepository.existsByClinicClinicIdAndPatientPatientId(
                northsideClinic().getClinicId(), patient.getPatientId())).isFalse();
    }

    @Test
    void clinicCanRejectWithNote() throws Exception {
        Patient patient = createPatient("PAT-TEST-REJECT-1", "Reject", "Patient");
        PatientLinkRequest request = createPendingRequest(patient, northsideClinic());

        MockHttpSession session = login("northadmin@clinicflow.local", "North@12345");
        mockMvc.perform(post("/clinic/link-requests/{id}/reject", request.getLinkRequestId())
                        .session(session).with(csrf())
                        .param("note", "Phone number did not match."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clinic/link-requests/" + request.getLinkRequestId()))
                .andExpect(flash().attributeExists("rejected"));

        PatientLinkRequest resolved = linkRequestRepository.findById(request.getLinkRequestId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(LinkRequestStatus.REJECTED);
        assertThat(resolved.getResolutionNote()).isEqualTo("Phone number did not match.");
        assertThat(resolved.getResolvedAt()).isNotNull();

        mockMvc.perform(get("/clinic/link-requests/{id}", request.getLinkRequestId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Rejected")))
                .andExpect(content().string(containsString("Phone number did not match.")));
    }

    @Test
    void approvalMergesAccountOntoExistingClinicRecord() throws Exception {
        User seededPatient = userRepository.findByEmailIgnoreCase("patient@clinicflow.local").orElseThrow();
        Clinic northside = northsideClinic();

        User mergeUser = new User();
        mergeUser.setEmail("merge-patient@example.test");
        mergeUser.setPasswordHash(seededPatient.getPasswordHash());
        mergeUser.setRole(Role.PATIENT);
        mergeUser.setStatus(UserStatus.ACTIVE);
        mergeUser.setEmailVerifiedAt(LocalDateTime.now(ZoneOffset.UTC));
        mergeUser.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        mergeUser.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        mergeUser = userRepository.save(mergeUser);

        Patient account = createPatient("PAT-TEST-MERGE-1", "Merge", "Patient");
        account.setUser(mergeUser);
        account.setEmail("merge-patient@example.test");
        account.setAddressLine1("44 Lake View Road");
        account.setAllergies("Penicillin");
        account = patientRepository.save(account);

        Patient clinicRecord = createPatient("PAT-TEST-MERGE-2", "Merge", "Patient");
        clinicRecord = patientRepository.save(clinicRecord);

        ClinicPatient link = new ClinicPatient();
        link.setClinic(northside);
        link.setPatient(clinicRecord);
        link.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        clinicPatientRepository.save(link);

        PatientLinkRequest request = createPendingRequest(account, northside);

        MockHttpSession mergeSession = login("merge-patient@example.test", "Patient@12345");
        mockMvc.perform(get("/patient/profile").session(mergeSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PAT-TEST-MERGE-1")));

        MockHttpSession clinicSession = login("northadmin@clinicflow.local", "North@12345");
        mockMvc.perform(post("/clinic/link-requests/{id}/approve", request.getLinkRequestId())
                        .session(clinicSession).with(csrf())
                        .param("identityVerified", "true")
                        .param("clinicPatientPublicId", clinicRecord.getPublicPatientId())
                        .param("note", "Merged duplicate."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clinic/link-requests/" + request.getLinkRequestId()))
                .andExpect(flash().attributeExists("approved"));

        Patient resolvedPatient = patientRepository.findByUserUserId(mergeUser.getUserId()).orElseThrow();
        assertThat(resolvedPatient.getPatientId()).isEqualTo(clinicRecord.getPatientId());
        assertThat(resolvedPatient.getPublicPatientId()).isEqualTo("PAT-TEST-MERGE-2");
        assertThat(resolvedPatient.getAddressLine1()).isEqualTo("44 Lake View Road");
        assertThat(resolvedPatient.getAllergies()).isEqualTo("Penicillin");
        assertThat(resolvedPatient.getEmail()).isEqualTo("merge-patient@example.test");
        assertThat(patientRepository.findById(account.getPatientId()).orElseThrow().getUser()).isNull();

        PatientLinkRequest resolved = linkRequestRepository.findById(request.getLinkRequestId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(LinkRequestStatus.APPROVED);
        assertThat(resolved.getPatient().getPatientId()).isEqualTo(clinicRecord.getPatientId());
        assertThat(resolved.getClinicPatientRecord().getPatientId()).isEqualTo(clinicRecord.getPatientId());

        mockMvc.perform(get("/patient/profile").session(mergeSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PAT-TEST-MERGE-2")))
                .andExpect(content().string(containsString("Northside Family Clinic")));
    }

    private Patient createPatient(String publicId, String firstName, String lastName) {
        Patient patient = new Patient();
        patient.setPublicPatientId(publicId);
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setDob(LocalDate.of(1990, 1, 1));
        patient.setEmail(firstName.toLowerCase() + "@example.test");
        return patientRepository.save(patient);
    }

    private PatientLinkRequest createPendingRequest(Patient patient, Clinic clinic) {
        PatientLinkRequest request = new PatientLinkRequest();
        request.setPatient(patient);
        request.setClinic(clinic);
        request.setMethod(LinkRequestMethod.CLINIC_APPROVAL);
        request.setStatus(LinkRequestStatus.PENDING);
        request.setAttempts(0);
        request.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        return linkRequestRepository.save(request);
    }

    private PatientLinkRequest pendingRequest(Long clinicId, Long patientId) {
        return linkRequestRepository
                .findByClinicClinicIdAndStatusOrderByCreatedAtDesc(clinicId, LinkRequestStatus.PENDING)
                .stream()
                .filter(request -> request.getPatient().getPatientId().equals(patientId))
                .findFirst()
                .orElseThrow();
    }

    private Clinic northsideClinic() {
        return clinicRepository.findAll().stream()
                .filter(clinic -> "Northside Family Clinic".equals(clinic.getName()))
                .findFirst()
                .orElseThrow();
    }

    private MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/login").with(csrf())
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
