package com.clinicflow.security;

import com.clinicflow.domain.enums.Role;

public final class RoleHomeResolver {

    private RoleHomeResolver() {
    }

    public static String homeFor(Role role) {
        return switch (role) {
            case SUPER_ADMIN -> "/admin/dashboard";
            case CLINIC_ADMIN -> "/clinic/dashboard";
            case DOCTOR -> "/doctor/dashboard";
            case PATIENT -> "/patient/dashboard";
            case PHARMACY_ADMIN, PHARMACIST -> "/pharmacy/dashboard";
        };
    }
}