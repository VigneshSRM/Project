package com.clinicflow.web.admin;

import com.clinicflow.domain.entity.Clinic;
import com.clinicflow.domain.entity.Pharmacy;
import com.clinicflow.domain.enums.ClinicStatus;
import com.clinicflow.domain.enums.PharmacyStatus;
import com.clinicflow.domain.enums.SmtpSecurity;
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
public class AdminController {

    private final AdminClinicService clinicService;
    private final AdminPharmacyService pharmacyService;
    private final AdminPatientService patientService;
    private final AdminReportService reportService;
    private final SmtpSettingsService smtpSettingsService;

    public AdminController(AdminClinicService clinicService, AdminPharmacyService pharmacyService,
                           AdminPatientService patientService, AdminReportService reportService,
                           SmtpSettingsService smtpSettingsService) {
        this.clinicService = clinicService;
        this.pharmacyService = pharmacyService;
        this.patientService = patientService;
        this.reportService = reportService;
        this.smtpSettingsService = smtpSettingsService;
    }

    @GetMapping("/admin/clinics")
    public String clinics(Model model) {
        model.addAttribute("clinics", clinicService.list());
        model.addAttribute("pageTitle", "Clinic Enrollment");
        return "admin/clinics";
    }

    @GetMapping("/admin/clinics/new")
    public String newClinic(Model model) {
        if (!model.containsAttribute("clinicForm")) {
            model.addAttribute("clinicForm", new ClinicEnrollmentForm());
        }
        model.addAttribute("pageTitle", "Enroll clinic");
        return "admin/clinic-form";
    }

    @PostMapping("/admin/clinics")
    public String enrollClinic(@Valid @ModelAttribute("clinicForm") ClinicEnrollmentForm form,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal AppUserPrincipal principal,
                               Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Enroll clinic");
            return "admin/clinic-form";
        }
        try {
            Clinic clinic = clinicService.enroll(form, principal.getUserId());
            redirectAttributes.addFlashAttribute("clinicCreated",
                    "Clinic enrolled. An invitation was sent to " + form.getAdminEmail() + ".");
            return "redirect:/admin/clinics/" + clinic.getClinicId();
        } catch (WeakPasswordException ex) {
            bindingResult.reject("invite.failed", ex.getMessage());
            model.addAttribute("pageTitle", "Enroll clinic");
            return "admin/clinic-form";
        }
    }

    @GetMapping("/admin/clinics/{id}")
    public String clinic(@PathVariable Long id, Model model) {
        AdminClinicService.ClinicDetail detail = clinicService.detail(id);
        model.addAttribute("detail", detail);
        model.addAttribute("pageTitle", detail.clinic().getName());
        return "admin/clinic-detail";
    }

    @PostMapping("/admin/clinics/{id}/status")
    public String clinicStatus(@PathVariable Long id, @RequestParam ClinicStatus status,
                               @AuthenticationPrincipal AppUserPrincipal principal,
                               RedirectAttributes redirectAttributes) {
        clinicService.changeStatus(id, status, principal.getUserId());
        redirectAttributes.addFlashAttribute("clinicUpdated", "Clinic status changed to " + status + ".");
        return "redirect:/admin/clinics/" + id;
    }

    @PostMapping("/admin/clinics/{id}/invite/resend")
    public String resendClinicInvite(@PathVariable Long id,
                                     @AuthenticationPrincipal AppUserPrincipal principal,
                                     RedirectAttributes redirectAttributes) {
        boolean sent = clinicService.resendInvite(id, principal.getUserId());
        redirectAttributes.addFlashAttribute(sent ? "clinicUpdated" : "clinicError",
                sent ? "Invitation re-sent." : "No pending clinic administrator invitation was found.");
        return "redirect:/admin/clinics/" + id;
    }

    @GetMapping("/admin/pharmacies")
    public String pharmacies(Model model) {
        model.addAttribute("pharmacies", pharmacyService.list());
        model.addAttribute("pageTitle", "Pharmacy Enrollment");
        return "admin/pharmacies";
    }

    @GetMapping("/admin/pharmacies/new")
    public String newPharmacy(Model model) {
        if (!model.containsAttribute("pharmacyForm")) {
            model.addAttribute("pharmacyForm", new PharmacyEnrollmentForm());
        }
        model.addAttribute("pageTitle", "Enroll pharmacy");
        return "admin/pharmacy-form";
    }

    @PostMapping("/admin/pharmacies")
    public String enrollPharmacy(@Valid @ModelAttribute("pharmacyForm") PharmacyEnrollmentForm form,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal AppUserPrincipal principal,
                                 Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Enroll pharmacy");
            return "admin/pharmacy-form";
        }
        try {
            Pharmacy pharmacy = pharmacyService.enroll(form, principal.getUserId());
            redirectAttributes.addFlashAttribute("pharmacyCreated",
                    "Pharmacy enrolled. An invitation was sent to " + form.getAdminEmail() + ".");
            return "redirect:/admin/pharmacies/" + pharmacy.getPharmacyId();
        } catch (WeakPasswordException ex) {
            bindingResult.reject("invite.failed", ex.getMessage());
            model.addAttribute("pageTitle", "Enroll pharmacy");
            return "admin/pharmacy-form";
        }
    }

    @GetMapping("/admin/pharmacies/{id}")
    public String pharmacy(@PathVariable Long id, Model model) {
        AdminPharmacyService.PharmacyDetail detail = pharmacyService.detail(id);
        model.addAttribute("detail", detail);
        model.addAttribute("pageTitle", detail.pharmacy().getName());
        return "admin/pharmacy-detail";
    }

    @PostMapping("/admin/pharmacies/{id}/status")
    public String pharmacyStatus(@PathVariable Long id, @RequestParam PharmacyStatus status,
                                 @AuthenticationPrincipal AppUserPrincipal principal,
                                 RedirectAttributes redirectAttributes) {
        pharmacyService.changeStatus(id, status, principal.getUserId());
        redirectAttributes.addFlashAttribute("pharmacyUpdated", "Pharmacy status changed to " + status + ".");
        return "redirect:/admin/pharmacies/" + id;
    }

    @PostMapping("/admin/pharmacies/{id}/invite/resend")
    public String resendPharmacyInvite(@PathVariable Long id,
                                       @AuthenticationPrincipal AppUserPrincipal principal,
                                       RedirectAttributes redirectAttributes) {
        boolean sent = pharmacyService.resendInvite(id, principal.getUserId());
        redirectAttributes.addFlashAttribute(sent ? "pharmacyUpdated" : "pharmacyError",
                sent ? "Invitation re-sent." : "No pending pharmacy administrator invitation was found.");
        return "redirect:/admin/pharmacies/" + id;
    }

    @GetMapping("/admin/patients")
    public String patients(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("patients", patientService.search(q));
        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("pageTitle", "Patient Masters");
        return "admin/patients";
    }

    @GetMapping("/admin/patients/{id}")
    public String patient(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                          Model model) {
        model.addAttribute("patient", patientService.detail(id, principal.getUserId()));
        model.addAttribute("pageTitle", "Patient record");
        return "admin/patient-detail";
    }

    @GetMapping("/admin/reports")
    public String reports(Model model) {
        model.addAttribute("report", reportService.view());
        model.addAttribute("pageTitle", "Platform Reports");
        return "admin/reports";
    }

    @GetMapping("/admin/smtp")
    public String smtp(@RequestParam(required = false) String test, Model model) {
        if (!model.containsAttribute("smtpForm")) {
            model.addAttribute("smtpForm", smtpSettingsService.view().form());
        }
        populateSmtp(model);
        model.addAttribute("pageTitle", "SMTP Settings");
        if ("sent".equals(test)) {
            model.addAttribute("smtpTestMessage", "Connection test completed. Check the result below.");
        }
        return "admin/smtp";
    }

    @PostMapping("/admin/smtp")
    public String saveSmtp(@Valid @ModelAttribute("smtpForm") SmtpSettingsForm form,
                           BindingResult bindingResult,
                           @AuthenticationPrincipal AppUserPrincipal principal,
                           Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateSmtp(model);
            model.addAttribute("pageTitle", "SMTP Settings");
            return "admin/smtp";
        }
        smtpSettingsService.save(form, principal.getUserId());
        redirectAttributes.addFlashAttribute("smtpSaved", "SMTP settings saved.");
        return "redirect:/admin/smtp";
    }

    @PostMapping("/admin/smtp/test")
    public String testSmtp(@Valid @ModelAttribute("smtpForm") SmtpSettingsForm form,
                           BindingResult bindingResult,
                           @AuthenticationPrincipal AppUserPrincipal principal,
                           Model model) {
        if (bindingResult.hasErrors()) {
            populateSmtp(model);
            model.addAttribute("pageTitle", "SMTP Settings");
            return "admin/smtp";
        }
        SmtpSettingsService.TestResult result = smtpSettingsService.test(form, principal.getUserId());
        model.addAttribute("smtpTestMessage", result.message());
        model.addAttribute("smtpTestSuccess", result.success());
        populateSmtp(model);
        model.addAttribute("pageTitle", "SMTP Settings");
        return "admin/smtp";
    }

    private void populateSmtp(Model model) {
        SmtpSettingsService.SmtpView view = smtpSettingsService.view();
        model.addAttribute("smtp", view);
        model.addAttribute("securityOptions", SmtpSecurity.values());
    }
}
