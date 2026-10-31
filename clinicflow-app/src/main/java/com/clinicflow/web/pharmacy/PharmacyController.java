package com.clinicflow.web.pharmacy;

import com.clinicflow.security.AppUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class PharmacyController {

    private final PharmacyService pharmacyService;

    public PharmacyController(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    @GetMapping("/pharmacy/verify")
    public String verifyPage(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        try {
            model.addAttribute("pharmacy", pharmacyService.requireUser(principal.getUserId())
                    .getPharmacy());
        } catch (IllegalStateException ex) {
            model.addAttribute("pharmacyError", ex.getMessage());
            model.addAttribute("pharmacy", null);
        }
        model.addAttribute("pageTitle", "Verify Prescription");
        return "pharmacy/verify";
    }

    @PostMapping("/pharmacy/verify")
    public String verify(@RequestParam String code,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         HttpServletRequest request, Model model) {
        try {
            model.addAttribute("pharmacy", pharmacyService.requireUser(principal.getUserId())
                    .getPharmacy());
            model.addAttribute("result", pharmacyService.verify(principal.getUserId(), code,
                    clientIp(request), principal.getUserId()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("pharmacyError", ex.getMessage());
            model.addAttribute("pharmacy", null);
        }
        model.addAttribute("pageTitle", "Verify Prescription");
        return "pharmacy/verify";
    }

    @GetMapping("/pharmacy/pos")
    public String posPage(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        addPosModel(model, principal.getUserId());
        model.addAttribute("pageTitle", "Pharmacy POS");
        return "pharmacy/pos";
    }

    @PostMapping("/pharmacy/pos")
    public String pos(@RequestParam String code,
                      @AuthenticationPrincipal AppUserPrincipal principal,
                      HttpServletRequest request, Model model) {
        addPosModel(model, principal.getUserId());
        try {
            model.addAttribute("result", pharmacyService.verify(principal.getUserId(), code,
                    clientIp(request), principal.getUserId()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("pharmacyError", ex.getMessage());
        }
        model.addAttribute("pageTitle", "Pharmacy POS");
        return "pharmacy/pos";
    }

    @GetMapping("/pharmacy/pos/result/{id}")
    public String posResult(@PathVariable UUID id,
                            @AuthenticationPrincipal AppUserPrincipal principal,
                            Model model, RedirectAttributes redirectAttributes) {
        addPosModel(model, principal.getUserId());
        try {
            model.addAttribute("result", pharmacyService.verification(principal.getUserId(), id));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("pharmacyError", ex.getMessage());
            return "redirect:/pharmacy/pos";
        }
        model.addAttribute("pageTitle", "Pharmacy POS");
        return "pharmacy/pos";
    }

    private void addPosModel(Model model, Long userId) {
        try {
            model.addAttribute("pharmacy", pharmacyService.requireUser(userId).getPharmacy());
            model.addAttribute("stats", pharmacyService.posStats(userId));
            model.addAttribute("priceMap", pharmacyService.priceRows(userId).stream()
                    .collect(Collectors.toMap(
                            row -> row.medicine().toLowerCase(Locale.ROOT),
                            PharmacyService.PriceRow::unitPrice, (first, second) -> second)));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("pharmacyError", ex.getMessage());
            model.addAttribute("pharmacy", null);
        }
    }

    @PostMapping("/pharmacy/dispense")
    public String dispense(@RequestParam UUID verificationId,
                           @RequestParam(required = false) String notes,
                           @RequestParam(required = false) BigDecimal discount,
                           @RequestParam(required = false) BigDecimal taxRate,
                           @RequestParam(required = false) String paymentMethod,
                           @RequestParam(required = false) BigDecimal amountTendered,
                           @RequestParam Map<String, String> params,
                           @AuthenticationPrincipal AppUserPrincipal principal,
                           RedirectAttributes redirectAttributes) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        Map<Long, BigDecimal> prices = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            try {
                if (entry.getKey().startsWith("qty_")) {
                    quantities.put(Long.parseLong(entry.getKey().substring(4)),
                            Integer.parseInt(entry.getValue().trim()));
                } else if (entry.getKey().startsWith("price_")) {
                    prices.put(Long.parseLong(entry.getKey().substring(6)),
                            new BigDecimal(entry.getValue().trim()));
                }
            } catch (NumberFormatException ex) {
                redirectAttributes.addFlashAttribute("dispenseError",
                        "Enter whole quantities and valid prices.");
                return backToResult(params, verificationId);
            }
        }
        try {
            PharmacyService.DispenseResult result = pharmacyService.dispense(principal.getUserId(),
                    verificationId, quantities, prices, discount, taxRate, paymentMethod,
                    amountTendered, notes, principal.getUserId());
            redirectAttributes.addFlashAttribute("dispenseQueued",
                    "Sale " + result.saleNumber() + " completed and income recorded.");
            return "redirect:/pharmacy/dispenses/" + result.dispenseId();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("dispenseError", ex.getMessage());
            return backToResult(params, verificationId);
        }
    }

    private static String backToResult(Map<String, String> params, UUID verificationId) {
        return "pos".equals(params.get("returnTo"))
                ? "redirect:/pharmacy/pos/result/" + verificationId
                : "redirect:/pharmacy/verify/result/" + verificationId;
    }

    @GetMapping("/pharmacy/verify/result/{id}")
    public String verificationResult(@PathVariable UUID id,
                                     @AuthenticationPrincipal AppUserPrincipal principal,
                                     Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("result", pharmacyService.verification(principal.getUserId(), id));
            model.addAttribute("pharmacy", pharmacyService.requireUser(principal.getUserId())
                    .getPharmacy());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("pharmacyError", ex.getMessage());
            return "redirect:/pharmacy/verify";
        }
        model.addAttribute("pageTitle", "Verify Prescription");
        return "pharmacy/verify";
    }

    @GetMapping("/pharmacy/dispenses/{id}")
    public String dispenseDetail(@PathVariable Long id,
                                 @AuthenticationPrincipal AppUserPrincipal principal,
                                 Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("dispense", pharmacyService.dispenseDetail(principal.getUserId(), id));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("pharmacyError", ex.getMessage());
            return "redirect:/pharmacy/verify";
        }
        model.addAttribute("pageTitle", "Sale Receipt");
        return "pharmacy/dispense-result";
    }

    @GetMapping("/pharmacy/prices")
    public String prices(@AuthenticationPrincipal AppUserPrincipal principal, Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("pharmacy", pharmacyService.requireUser(principal.getUserId())
                    .getPharmacy());
            model.addAttribute("prices", pharmacyService.priceRows(principal.getUserId()));
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("pharmacyError", ex.getMessage());
            return "redirect:/pharmacy/dashboard";
        }
        model.addAttribute("pageTitle", "Medicine Price List");
        return "pharmacy/prices";
    }

    @PostMapping("/pharmacy/prices")
    public String savePrice(@RequestParam(required = false) Long priceId,
                            @RequestParam String medicine,
                            @RequestParam BigDecimal unitPrice,
                            @AuthenticationPrincipal AppUserPrincipal principal,
                            RedirectAttributes redirectAttributes) {
        try {
            pharmacyService.savePrice(principal.getUserId(), priceId, medicine, unitPrice,
                    principal.getUserId());
            redirectAttributes.addFlashAttribute("priceSaved", "Price saved.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("priceError", ex.getMessage());
        }
        return "redirect:/pharmacy/prices";
    }

    @PostMapping("/pharmacy/prices/{id}/delete")
    public String deletePrice(@PathVariable Long id,
                              @AuthenticationPrincipal AppUserPrincipal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            pharmacyService.deletePrice(principal.getUserId(), id, principal.getUserId());
            redirectAttributes.addFlashAttribute("priceSaved", "Price entry deleted.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("priceError", ex.getMessage());
        }
        return "redirect:/pharmacy/prices";
    }

    @GetMapping("/pharmacy/users")
    public String users(@AuthenticationPrincipal AppUserPrincipal principal, Model model,
                        RedirectAttributes redirectAttributes) {
        try {
            var current = pharmacyService.requireUser(principal.getUserId());
            model.addAttribute("users", pharmacyService.users(principal.getUserId()));
            model.addAttribute("pharmacy", current.getPharmacy());
            model.addAttribute("canManage", current.getRole()
                    == com.clinicflow.domain.enums.PharmacyUserRole.PHARMACY_ADMIN
                    && current.getStatus() == com.clinicflow.domain.enums.PharmacyUserStatus.ACTIVE);
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("pharmacyError", ex.getMessage());
            return "redirect:/pharmacy/dashboard";
        }
        model.addAttribute("pageTitle", "Pharmacy Users");
        return "pharmacy/users";
    }

    @PostMapping("/pharmacy/users/invite")
    public String invite(@RequestParam String email,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            pharmacyService.invitePharmacist(principal.getUserId(), email, principal.getUserId());
            redirectAttributes.addFlashAttribute("userSaved",
                    "Invitation sent. The pharmacist chooses a password through the emailed link.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("userError", ex.getMessage());
        }
        return "redirect:/pharmacy/users";
    }

    @PostMapping("/pharmacy/users/{id}/resend")
    public String resend(@PathVariable Long id,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            pharmacyService.resendInvitation(principal.getUserId(), id, principal.getUserId());
            redirectAttributes.addFlashAttribute("userSaved", "Invitation resent.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("userError", ex.getMessage());
        }
        return "redirect:/pharmacy/users";
    }

    @PostMapping("/pharmacy/users/{id}/status")
    public String status(@PathVariable Long id, @RequestParam boolean enabled,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            pharmacyService.setUserEnabled(principal.getUserId(), id, enabled,
                    principal.getUserId());
            redirectAttributes.addFlashAttribute("userSaved",
                    enabled ? "Pharmacy user enabled." : "Pharmacy user disabled.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("userError", ex.getMessage());
        }
        return "redirect:/pharmacy/users";
    }

    @GetMapping("/pharmacy/reports")
    public String reports(@AuthenticationPrincipal AppUserPrincipal principal, Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("report", pharmacyService.report(principal.getUserId()));
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("pharmacyError", ex.getMessage());
            return "redirect:/pharmacy/dashboard";
        }
        model.addAttribute("pageTitle", "Pharmacy Reports");
        return "pharmacy/reports";
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
