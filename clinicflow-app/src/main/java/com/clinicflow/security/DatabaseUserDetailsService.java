package com.clinicflow.security;

import com.clinicflow.domain.entity.Patient;
import com.clinicflow.domain.entity.PharmacyUser;
import com.clinicflow.domain.entity.User;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.repository.DoctorUserLinkRepository;
import com.clinicflow.domain.repository.PatientRepository;
import com.clinicflow.domain.repository.PharmacyUserRepository;
import com.clinicflow.domain.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PharmacyUserRepository pharmacyUserRepository;
    private final DoctorUserLinkRepository doctorUserLinkRepository;

    public DatabaseUserDetailsService(UserRepository userRepository,
                                      PatientRepository patientRepository,
                                      PharmacyUserRepository pharmacyUserRepository,
                                      DoctorUserLinkRepository doctorUserLinkRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.pharmacyUserRepository = pharmacyUserRepository;
        this.doctorUserLinkRepository = doctorUserLinkRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + username));

        return new AppUserPrincipal(
                user.getUserId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.getStatus(),
                user.getClinic() != null ? user.getClinic().getClinicId() : null,
                resolveDisplayName(user)
        );
    }

    private String resolveDisplayName(User user) {
        if (user.getRole() == Role.PATIENT) {
            return patientRepository.findByUserUserId(user.getUserId())
                    .map(Patient::getDisplayName)
                    .orElse(user.getEmail());
        }
        if (user.getRole() == Role.DOCTOR) {
            return doctorUserLinkRepository.findByUserUserId(user.getUserId())
                    .map(link -> link.getDoctor().getName())
                    .orElse(user.getEmail());
        }
        if (user.getRole() == Role.PHARMACY_ADMIN || user.getRole() == Role.PHARMACIST) {
            return pharmacyUserRepository.findByUserUserId(user.getUserId())
                    .map(PharmacyUser::getPharmacy)
                    .map(p -> p.getName())
                    .orElse(user.getEmail());
        }
        if (user.getClinic() != null) {
            return user.getClinic().getName();
        }
        if (user.getRole() == Role.SUPER_ADMIN) {
            return "Platform Admin";
        }
        return user.getEmail();
    }
}