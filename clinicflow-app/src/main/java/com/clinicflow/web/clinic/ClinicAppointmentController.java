package com.clinicflow.web.clinic;

import com.clinicflow.domain.enums.AppointmentStatus;
import com.clinicflow.security.AppUserPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class ClinicAppointmentController {

    private final ClinicAppointmentService appointmentService;

    public ClinicAppointmentController(ClinicAppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/clinic/appointments")
    public String list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam(required = false) AppointmentStatus status,
                       @RequestParam(required = false, defaultValue = "day") String scope,
                       @AuthenticationPrincipal AppUserPrincipal principal,
                       Model model) {
        boolean allDates = "all".equals(scope);
        LocalDate selected = allDates ? null : (date == null ? LocalDate.now() : date);
        model.addAttribute("appointments", appointmentService.list(principal.getClinicId(), selected, status));
        model.addAttribute("selectedDate", selected);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("allDates", allDates);
        model.addAttribute("statuses", AppointmentStatus.values());
        model.addAttribute("pageTitle", "Appointments");
        return "clinic/appointments";
    }

    @GetMapping("/clinic/appointments/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("appointment", appointmentService.detail(principal.getClinicId(), id,
                    principal.getUserId()));
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("appointmentError", ex.getMessage());
            return "redirect:/clinic/appointments";
        }
        model.addAttribute("pageTitle", "Appointment");
        return "clinic/appointment-detail";
    }

    @PostMapping("/clinic/appointments/{id}/action")
    public String action(@PathVariable Long id, @RequestParam String action,
                         @RequestParam(required = false) String reason,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            appointmentService.transition(principal.getClinicId(), id, action, reason,
                    principal.getUserId());
            redirectAttributes.addFlashAttribute("appointmentSaved", "Appointment updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("appointmentError", ex.getMessage());
        }
        return "redirect:/clinic/appointments/" + id;
    }
}
