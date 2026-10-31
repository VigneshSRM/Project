package com.clinicflow.web.pharmacy;

import com.clinicflow.security.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
public class PharmacyFinanceController {

    private final PharmacyFinanceService financeService;

    public PharmacyFinanceController(PharmacyFinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/pharmacy/finance")
    public String page(@AuthenticationPrincipal AppUserPrincipal principal, Model model,
                       RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("pharmacy", financeService.pharmacy(principal.getUserId()));
            model.addAttribute("entries", financeService.list(principal.getUserId()));
            LocalDate today = LocalDate.now();
            LocalDate from = today.minusDays(30);
            BigDecimal income = financeService.income(principal.getUserId(), from, today);
            BigDecimal expense = financeService.expense(principal.getUserId(), from, today);
            model.addAttribute("income", income);
            model.addAttribute("expense", expense);
            model.addAttribute("net", income.subtract(expense));
            model.addAttribute("incomeToday", financeService.income(principal.getUserId(), today, today));
            model.addAttribute("expenseToday", financeService.expense(principal.getUserId(), today, today));
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("financeError", ex.getMessage());
            return "redirect:/pharmacy/dashboard";
        }
        model.addAttribute("pageTitle", "Finance");
        return "pharmacy/finance";
    }

    @PostMapping("/pharmacy/finance")
    public String add(@RequestParam String entryType,
                      @RequestParam BigDecimal amount,
                      @RequestParam(required = false) String description,
                      @RequestParam(required = false) LocalDate entryDate,
                      @AuthenticationPrincipal AppUserPrincipal principal,
                      RedirectAttributes redirectAttributes) {
        try {
            financeService.addEntry(principal.getUserId(), entryType, amount, description,
                    entryDate, principal.getUserId());
            redirectAttributes.addFlashAttribute("financeSaved", "Finance entry added.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("financeError", ex.getMessage());
        }
        return "redirect:/pharmacy/finance";
    }
}
