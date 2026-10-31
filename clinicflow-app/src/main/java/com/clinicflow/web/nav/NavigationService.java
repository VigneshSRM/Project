package com.clinicflow.web.nav;

import com.clinicflow.domain.enums.Role;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NavigationService {

    public List<NavItem> itemsFor(Role role) {
        return switch (role) {
            case SUPER_ADMIN -> List.of(
                    new NavItem("Dashboard", "/admin/dashboard", "layout-dashboard", "/admin/dashboard"),
                    new NavItem("Clinic Enrollment", "/admin/clinics", "building-2", "/admin/clinics"),
                    new NavItem("Pharmacy Enrollment", "/admin/pharmacies", "pill", "/admin/pharmacies"),
                    new NavItem("Patient Masters", "/admin/patients", "users", "/admin/patients"),
                    new NavItem("Reports", "/admin/reports", "bar-chart-3", "/admin/reports"),
                    new NavItem("SMTP Settings", "/admin/smtp", "mail-cog", "/admin/smtp")
            );
            case CLINIC_ADMIN -> List.of(
                    new NavItem("Dashboard", "/clinic/dashboard", "layout-dashboard", "/clinic/dashboard"),
                    new NavItem("Patient Masters", "/clinic/patients", "users", "/clinic/patients"),
                    new NavItem("Link Requests", "/clinic/link-requests", "shield-check", "/clinic/link-requests"),
                    new NavItem("Appointments", "/clinic/appointments", "calendar-check", "/clinic/appointments"),
                    new NavItem("Doctors", "/clinic/doctors", "stethoscope", "/clinic/doctors"),
                    new NavItem("Inventory", "/clinic/inventory", "pill", "/clinic/inventory"),
                    new NavItem("Prescriptions", "/clinic/prescriptions", "file-text", "/clinic/prescriptions")
            );
            case DOCTOR -> List.of(
                    new NavItem("Dashboard", "/doctor/dashboard", "layout-dashboard", "/doctor/dashboard"),
                    new NavItem("My Appointments", "/doctor/appointments", "calendar-check", "/doctor/appointments"),
                    new NavItem("Prescriptions", "/doctor/prescriptions", "file-text", "/doctor/prescriptions")
            );
            case PATIENT -> List.of(
                    new NavItem("Dashboard", "/patient/dashboard", "layout-dashboard", "/patient/dashboard"),
                    new NavItem("My Profile", "/patient/profile", "user", "/patient/profile"),
                    new NavItem("Find Nearby Clinics", "/patient/clinics", "map-pin", "/patient/clinics"),
                    new NavItem("Appointments", "/patient/appointments", "calendar", "/patient/appointments"),
                    new NavItem("My Prescriptions", "/patient/prescriptions", "file-text", "/patient/prescriptions"),
                    new NavItem("My Medical History", "/patient/history", "history", "/patient/history")
            );
            case PHARMACY_ADMIN, PHARMACIST -> List.of(
                    new NavItem("Dashboard", "/pharmacy/dashboard", "layout-dashboard", "/pharmacy/dashboard"),
                    new NavItem("POS", "/pharmacy/pos", "scan-search", "/pharmacy/pos"),
                    new NavItem("Price List", "/pharmacy/prices", "file-text", "/pharmacy/prices"),
                    new NavItem("Finance", "/pharmacy/finance", "bar-chart-3", "/pharmacy/finance"),
                    new NavItem("Users", "/pharmacy/users", "users", "/pharmacy/users"),
                    new NavItem("Reports", "/pharmacy/reports", "bar-chart-3", "/pharmacy/reports")
            );
        };
    }

    public List<Breadcrumb> breadcrumbsFor(Role role, String currentPath) {
        return breadcrumbsFor(role, currentPath, null);
    }

    public List<Breadcrumb> breadcrumbsFor(Role role, String currentPath, String currentLabel) {
        List<Breadcrumb> crumbs = new ArrayList<>();
        crumbs.add(new Breadcrumb("Home", homeFor(role)));
        if (currentPath == null || currentPath.equals(homeFor(role))) {
            crumbs.add(new Breadcrumb("Dashboard", null));
            return crumbs;
        }
        NavItem parent = itemsFor(role).stream()
                .filter(item -> item.isActive(currentPath))
                .findFirst()
                .orElse(null);
        if (parent == null && role == Role.PATIENT && currentPath != null
                && currentPath.startsWith("/patient/book")) {
            parent = itemsFor(role).stream()
                    .filter(item -> item.href().equals("/patient/clinics"))
                    .findFirst().orElse(null);
        }
        if (parent != null && !parent.href().equals(currentPath)) {
            crumbs.add(new Breadcrumb(parent.label(), parent.href()));
        }
        crumbs.add(new Breadcrumb(currentLabel != null ? currentLabel : titleFor(role, currentPath), null));
        return crumbs;
    }

    public String homeFor(Role role) {
        return switch (role) {
            case SUPER_ADMIN -> "/admin/dashboard";
            case CLINIC_ADMIN -> "/clinic/dashboard";
            case DOCTOR -> "/doctor/dashboard";
            case PATIENT -> "/patient/dashboard";
            case PHARMACY_ADMIN, PHARMACIST -> "/pharmacy/dashboard";
        };
    }

    public String roleLabel(Role role) {
        return switch (role) {
            case SUPER_ADMIN -> "Super Admin";
            case CLINIC_ADMIN -> "Clinic Administration";
            case DOCTOR -> "Doctor Workspace";
            case PATIENT -> "Patient Portal";
            case PHARMACY_ADMIN, PHARMACIST -> "Pharmacy Workspace";
        };
    }

    private String titleFor(Role role, String path) {
        return itemsFor(role).stream()
                .filter(item -> item.isActive(path))
                .map(NavItem::label)
                .findFirst()
                .orElse("Page");
    }
}