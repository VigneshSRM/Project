package com.clinicflow.web.clinic;

import com.clinicflow.domain.enums.Gender;
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
public class ClinicPatientController {

    private final ClinicPatientService patientService;

    public ClinicPatientController(ClinicPatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/clinic/patients")
    public String list(@RequestParam(required = false) String q,
                       @AuthenticationPrincipal AppUserPrincipal principal,
                       Model model) {
        model.addAttribute("patients", patientService.list(principal.getClinicId(), q));
        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("pageTitle", "Patient Masters");
        return "clinic/patients";
    }

    @GetMapping("/clinic/patients/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("patientForm")) {
            model.addAttribute("patientForm", new PatientForm());
        }
        model.addAttribute("genders", Gender.values());
        model.addAttribute("patientId", (Long) null);
        model.addAttribute("editing", false);
        model.addAttribute("pageTitle", "New patient");
        return "clinic/patient-form";
    }

    @PostMapping("/clinic/patients")
    public String create(@Valid @ModelAttribute("patientForm") PatientForm form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("genders", Gender.values());
            model.addAttribute("patientId", (Long) null);
            model.addAttribute("editing", false);
            model.addAttribute("pageTitle", "New patient");
            return "clinic/patient-form";
        }
        Long id = patientService.create(principal.getClinicId(), form, principal.getUserId());
        redirectAttributes.addFlashAttribute("patientSaved", "Patient record created.");
        return "redirect:/clinic/patients/" + id;
    }

    @GetMapping("/clinic/patients/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("patient", patientService.detail(principal.getClinicId(), id,
                    principal.getUserId()));
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("patientError", ex.getMessage());
            return "redirect:/clinic/patients";
        }
        model.addAttribute("pageTitle", "Patient record");
        return "clinic/patient-detail";
    }

    @GetMapping("/clinic/patients/{id}/edit")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                           Model model, RedirectAttributes redirectAttributes) {
        try {
            if (!model.containsAttribute("patientForm")) {
                model.addAttribute("patientForm", patientService.formFor(principal.getClinicId(), id));
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("patientError", ex.getMessage());
            return "redirect:/clinic/patients";
        }
        model.addAttribute("patientId", id);
        model.addAttribute("genders", Gender.values());
        model.addAttribute("editing", true);
        model.addAttribute("pageTitle", "Edit patient");
        return "clinic/patient-form";
    }

    @PostMapping("/clinic/patients/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("patientForm") PatientForm form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("patientId", id);
            model.addAttribute("genders", Gender.values());
            model.addAttribute("editing", true);
            model.addAttribute("pageTitle", "Edit patient");
            return "clinic/patient-form";
        }
        try {
            patientService.update(principal.getClinicId(), id, form, principal.getUserId());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("patientError", ex.getMessage());
            return "redirect:/clinic/patients";
        }
        redirectAttributes.addFlashAttribute("patientSaved", "Patient record updated.");
        return "redirect:/clinic/patients/" + id;
    }

    @PostMapping("/clinic/patients/link")
    public String linkExisting(@RequestParam String publicPatientId,
                               @AuthenticationPrincipal AppUserPrincipal principal,
                               RedirectAttributes redirectAttributes) {
        try {
            var patient = patientService.linkExisting(principal.getClinicId(), publicPatientId,
                    principal.getUserId());
            redirectAttributes.addFlashAttribute("patientSaved", "Existing patient linked to this clinic.");
            return "redirect:/clinic/patients/" + patient.getPatientId();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("patientError", ex.getMessage());
            return "redirect:/clinic/patients";
        }
    }
}
