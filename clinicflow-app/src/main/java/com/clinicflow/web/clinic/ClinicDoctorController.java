package com.clinicflow.web.clinic;

import com.clinicflow.domain.enums.DoctorStatus;
import com.clinicflow.identity.WeakPasswordException;
import com.clinicflow.security.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ClinicDoctorController {

    private final ClinicDoctorService doctorService;

    public ClinicDoctorController(ClinicDoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping("/clinic/doctors")
    public String list(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("doctors", doctorService.list(principal.getClinicId()));
        model.addAttribute("pageTitle", "Doctors");
        return "clinic/doctors";
    }

    @GetMapping("/clinic/doctors/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("doctorForm")) {
            model.addAttribute("doctorForm", new DoctorForm());
        }
        model.addAttribute("statuses", DoctorStatus.values());
        model.addAttribute("doctorId", (Long) null);
        model.addAttribute("editing", false);
        model.addAttribute("pageTitle", "New doctor");
        return "clinic/doctor-form";
    }

    @PostMapping("/clinic/doctors")
    public String create(@Valid @ModelAttribute("doctorForm") DoctorForm form, BindingResult bindingResult,
                         @AuthenticationPrincipal AppUserPrincipal principal, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("statuses", DoctorStatus.values());
            model.addAttribute("doctorId", (Long) null);
            model.addAttribute("editing", false);
            model.addAttribute("pageTitle", "New doctor");
            return "clinic/doctor-form";
        }
        try {
            Long id = doctorService.create(principal.getClinicId(), form, principal.getUserId());
            redirectAttributes.addFlashAttribute("doctorSaved", "Doctor created.");
            return "redirect:/clinic/doctors/" + id;
        } catch (WeakPasswordException ex) {
            bindingResult.reject("invite.failed", ex.getMessage());
            model.addAttribute("statuses", DoctorStatus.values());
            model.addAttribute("doctorId", (Long) null);
            model.addAttribute("editing", false);
            model.addAttribute("pageTitle", "New doctor");
            return "clinic/doctor-form";
        }
    }

    @GetMapping("/clinic/doctors/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("doctor", doctorService.detail(principal.getClinicId(), id));
            if (!model.containsAttribute("scheduleForm")) {
                model.addAttribute("scheduleForm", new ScheduleForm());
            }
            model.addAttribute("weekdays", weekdays());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("doctorError", ex.getMessage());
            return "redirect:/clinic/doctors";
        }
        model.addAttribute("pageTitle", "Doctor");
        return "clinic/doctor-detail";
    }

    @GetMapping("/clinic/doctors/{id}/edit")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                           Model model, RedirectAttributes redirectAttributes) {
        try {
            if (!model.containsAttribute("doctorForm")) {
                model.addAttribute("doctorForm", doctorService.formFor(principal.getClinicId(), id));
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("doctorError", ex.getMessage());
            return "redirect:/clinic/doctors";
        }
        model.addAttribute("doctorId", id);
        model.addAttribute("statuses", DoctorStatus.values());
        model.addAttribute("editing", true);
        model.addAttribute("pageTitle", "Edit doctor");
        return "clinic/doctor-form";
    }

    @PostMapping("/clinic/doctors/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("doctorForm") DoctorForm form,
                         BindingResult bindingResult, @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("doctorId", id);
            model.addAttribute("statuses", DoctorStatus.values());
            model.addAttribute("editing", true);
            model.addAttribute("pageTitle", "Edit doctor");
            return "clinic/doctor-form";
        }
        try {
            doctorService.update(principal.getClinicId(), id, form, principal.getUserId());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("doctorError", ex.getMessage());
            return "redirect:/clinic/doctors";
        }
        redirectAttributes.addFlashAttribute("doctorSaved", "Doctor updated.");
        return "redirect:/clinic/doctors/" + id;
    }

    @PostMapping("/clinic/doctors/{id}/schedules")
    public String addSchedule(@PathVariable Long id,
                              @Valid @ModelAttribute("scheduleForm") ScheduleForm form,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal AppUserPrincipal principal,
                              Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("doctor", doctorService.detail(principal.getClinicId(), id));
            model.addAttribute("weekdays", weekdays());
            model.addAttribute("pageTitle", "Doctor");
            return "clinic/doctor-detail";
        }
        try {
            doctorService.addSchedule(principal.getClinicId(), id, form, principal.getUserId());
            redirectAttributes.addFlashAttribute("doctorSaved", "Schedule added.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("doctorError", ex.getMessage());
        }
        return "redirect:/clinic/doctors/" + id;
    }

    @PostMapping("/clinic/doctors/{id}/schedules/{scheduleId}/delete")
    public String deleteSchedule(@PathVariable Long id, @PathVariable Long scheduleId,
                                 @AuthenticationPrincipal AppUserPrincipal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            doctorService.deleteSchedule(principal.getClinicId(), id, scheduleId, principal.getUserId());
            redirectAttributes.addFlashAttribute("doctorSaved", "Schedule removed.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("doctorError", ex.getMessage());
        }
        return "redirect:/clinic/doctors/" + id;
    }

    @PostMapping("/clinic/doctors/{id}/invite/resend")
    public String resend(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        boolean sent = doctorService.resendInvite(principal.getClinicId(), id, principal.getUserId());
        redirectAttributes.addFlashAttribute(sent ? "doctorSaved" : "doctorError",
                sent ? "Invitation re-sent." : "No pending doctor invitation was found.");
        return "redirect:/clinic/doctors/" + id;
    }

    private static java.util.List<WeekdayOption> weekdays() {
        return java.util.List.of(new WeekdayOption(1, "Monday"), new WeekdayOption(2, "Tuesday"),
                new WeekdayOption(3, "Wednesday"), new WeekdayOption(4, "Thursday"),
                new WeekdayOption(5, "Friday"), new WeekdayOption(6, "Saturday"),
                new WeekdayOption(7, "Sunday"));
    }

    public record WeekdayOption(int value, String label) {
    }
}
