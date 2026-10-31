package com.clinicflow.web.clinic;

import com.clinicflow.security.AppUserPrincipal;
import com.clinicflow.web.nav.NavigationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clinic/link-requests")
public class ClinicLinkRequestController {

    private final ClinicLinkRequestService linkRequestService;
    private final NavigationService navigationService;

    public ClinicLinkRequestController(ClinicLinkRequestService linkRequestService,
                                       NavigationService navigationService) {
        this.linkRequestService = linkRequestService;
        this.navigationService = navigationService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        Long clinicId = clinicId(principal);
        if (clinicId == null) {
            return "redirect:/login";
        }
        model.addAttribute("view", linkRequestService.list(clinicId));
        model.addAttribute("pageTitle", "Link requests");
        return "clinic/link-requests";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         HttpServletRequest request,
                         Model model) {
        Long clinicId = clinicId(principal);
        if (clinicId == null) {
            return "redirect:/login";
        }
        ClinicLinkRequestDetailView view = detailOrNotFound(clinicId, id);
        addDetailModel(model, view, principal, request);
        return "clinic/link-request-detail";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @Valid @ModelAttribute("approvalForm") LinkApprovalForm form,
                          BindingResult bindingResult,
                          @AuthenticationPrincipal AppUserPrincipal principal,
                          HttpServletRequest request,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        Long clinicId = clinicId(principal);
        if (clinicId == null) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            addDetailModel(model, detailOrNotFound(clinicId, id), principal, request);
            return "clinic/link-request-detail";
        }

        ClinicLinkRequestService.DecisionResult result = linkRequestService.approve(
                clinicId, id, principal.getUserId(), form.getClinicPatientPublicId(), form.getNote());
        if (!result.success()) {
            model.addAttribute("decisionError", result.message());
            addDetailModel(model, detailOrNotFound(clinicId, id), principal, request);
            return "clinic/link-request-detail";
        }

        redirectAttributes.addFlashAttribute("approved", result.message());
        return "redirect:/clinic/link-requests/" + id;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @Valid @ModelAttribute("rejectionForm") LinkRejectionForm form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         HttpServletRequest request,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Long clinicId = clinicId(principal);
        if (clinicId == null) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            addDetailModel(model, detailOrNotFound(clinicId, id), principal, request);
            return "clinic/link-request-detail";
        }

        ClinicLinkRequestService.DecisionResult result = linkRequestService.reject(
                clinicId, id, principal.getUserId(), form.getNote());
        if (!result.success()) {
            model.addAttribute("decisionError", result.message());
            addDetailModel(model, detailOrNotFound(clinicId, id), principal, request);
            return "clinic/link-request-detail";
        }

        redirectAttributes.addFlashAttribute("rejected", result.message());
        return "redirect:/clinic/link-requests/" + id;
    }

    private ClinicLinkRequestDetailView detailOrNotFound(Long clinicId, Long id) {
        return linkRequestService.detail(clinicId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void addDetailModel(Model model, ClinicLinkRequestDetailView view,
                                AppUserPrincipal principal, HttpServletRequest request) {
        model.addAttribute("view", view);
        model.addAttribute("pageTitle", "Link request #" + view.id());
        model.addAttribute("breadcrumbs", navigationService.breadcrumbsFor(
                principal.getRole(), request.getRequestURI(), "Link request #" + view.id()));
        if (!model.containsAttribute("approvalForm")) {
            model.addAttribute("approvalForm", new LinkApprovalForm());
        }
        if (!model.containsAttribute("rejectionForm")) {
            model.addAttribute("rejectionForm", new LinkRejectionForm());
        }
    }

    private Long clinicId(AppUserPrincipal principal) {
        return principal == null ? null : principal.getClinicId();
    }
}
