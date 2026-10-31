package com.clinicflow.web.patient;

import com.clinicflow.domain.entity.Clinic;
import com.clinicflow.domain.entity.Patient;
import com.clinicflow.domain.entity.PatientLinkRequest;
import com.clinicflow.domain.entity.User;
import com.clinicflow.domain.enums.LinkRequestMethod;
import com.clinicflow.domain.enums.LinkRequestStatus;
import com.clinicflow.domain.repository.ClinicRepository;
import com.clinicflow.domain.repository.PatientLinkRequestRepository;
import com.clinicflow.domain.repository.PatientRepository;
import com.clinicflow.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class PatientProfileWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClinicRepository clinicRepository;

    @Autowired
    private PatientLinkRequestRepository linkRequestRepository;

    @Test
    void profileRendersLinkRequestHistory() throws Exception {
        User patientUser = userRepository.findByEmailIgnoreCase("patient@clinicflow.local").orElseThrow();
        Patient patient = patientRepository.findByUserUserId(patientUser.getUserId()).orElseThrow();
        Clinic clinic = clinicRepository.findAll().stream()
                .filter(candidate -> "City Care Clinic".equals(candidate.getName()))
                .findFirst()
                .orElseThrow();

        PatientLinkRequest request = new PatientLinkRequest();
        request.setPatient(patient);
        request.setClinic(clinic);
        request.setMethod(LinkRequestMethod.CLINIC_APPROVAL);
        request.setStatus(LinkRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        linkRequestRepository.save(request);

        MockHttpSession session = login("patient@clinicflow.local", "Patient@12345");

        mockMvc.perform(get("/patient/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Link requests")))
                .andExpect(content().string(containsString("Clinic approval")))
                .andExpect(content().string(containsString("Pending")));
    }

    @Test
    void patientCanViewAndUpdateOwnProfile() throws Exception {
        MockHttpSession session = login("patient@clinicflow.local", "Patient@12345");

        mockMvc.perform(get("/patient/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("My Profile")))
                .andExpect(content().string(containsString("PAT-")))
                .andExpect(content().string(containsString("City Care Clinic")))
                .andExpect(content().string(containsString("name=\"firstName\"")));

        mockMvc.perform(post("/patient/profile").session(session).with(csrf())
                        .param("firstName", "Riya")
                        .param("lastName", "Sharma")
                        .param("gender", "FEMALE")
                        .param("addressLine1", "14 Updated Street")
                        .param("city", "Pune")
                        .param("state", "Maharashtra")
                        .param("postalCode", "411001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/profile"));
    }

    @Test
    void doctorCannotOpenPatientProfile() throws Exception {
        MockHttpSession session = login("doctor@clinicflow.local", "Doctor@12345");

        mockMvc.perform(get("/patient/profile").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void patientCannotOpenClinicMasters() throws Exception {
        MockHttpSession session = login("patient@clinicflow.local", "Patient@12345");

        mockMvc.perform(get("/clinic/patients").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/patient/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
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
