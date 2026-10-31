package com.clinicflow.security;

import com.clinicflow.domain.enums.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleHomeResolverTest {

    @Test
    void everyRoleHasADashboard() {
        assertThat(RoleHomeResolver.homeFor(Role.SUPER_ADMIN)).isEqualTo("/admin/dashboard");
        assertThat(RoleHomeResolver.homeFor(Role.CLINIC_ADMIN)).isEqualTo("/clinic/dashboard");
        assertThat(RoleHomeResolver.homeFor(Role.DOCTOR)).isEqualTo("/doctor/dashboard");
        assertThat(RoleHomeResolver.homeFor(Role.PATIENT)).isEqualTo("/patient/dashboard");
        assertThat(RoleHomeResolver.homeFor(Role.PHARMACY_ADMIN)).isEqualTo("/pharmacy/dashboard");
        assertThat(RoleHomeResolver.homeFor(Role.PHARMACIST)).isEqualTo("/pharmacy/dashboard");
    }
}