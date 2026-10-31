package com.clinicflow.web.clinic;

import com.clinicflow.security.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ClinicInventoryController {

    private final ClinicInventoryService inventoryService;

    public ClinicInventoryController(ClinicInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/clinic/inventory")
    public String list(@AuthenticationPrincipal AppUserPrincipal principal, Model model,
                       RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("medicines", inventoryService.list(principal.getClinicId()));
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("inventoryError", ex.getMessage());
            return "redirect:/clinic/dashboard";
        }
        model.addAttribute("pageTitle", "Inventory");
        return "clinic/inventory";
    }

    @PostMapping("/clinic/inventory")
    public String add(@RequestParam String medicine,
                      @RequestParam(required = false) String dosage,
                      @RequestParam(required = false) String defaultFrequency,
                      @RequestParam(required = false) String defaultDuration,
                      @RequestParam(required = false) Integer defaultQuantity,
                      @RequestParam(required = false) String defaultForm,
                      @RequestParam(required = false) String defaultInstructions,
                      @AuthenticationPrincipal AppUserPrincipal principal,
                      RedirectAttributes redirectAttributes) {
        try {
            inventoryService.add(principal.getClinicId(), medicine, dosage, defaultFrequency,
                    defaultDuration, defaultQuantity, defaultForm, defaultInstructions);
            redirectAttributes.addFlashAttribute("inventorySaved", "Inventory item added.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("inventoryError", ex.getMessage());
        }
        return "redirect:/clinic/inventory";
    }

    @GetMapping("/clinic/inventory/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal AppUserPrincipal principal,
                           Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("medicine", inventoryService.get(principal.getClinicId(), id));
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("inventoryError", ex.getMessage());
            return "redirect:/clinic/inventory";
        }
        model.addAttribute("pageTitle", "Edit inventory item");
        return "clinic/inventory-edit";
    }

    @PostMapping("/clinic/inventory/{id}/edit")
    public String edit(@PathVariable Long id,
                       @RequestParam String medicine,
                       @RequestParam(required = false) String dosage,
                       @RequestParam(required = false) String defaultFrequency,
                       @RequestParam(required = false) String defaultDuration,
                       @RequestParam(required = false) Integer defaultQuantity,
                       @RequestParam(required = false) String defaultForm,
                       @RequestParam(required = false) String defaultInstructions,
                       @AuthenticationPrincipal AppUserPrincipal principal,
                       RedirectAttributes redirectAttributes) {
        try {
            inventoryService.update(principal.getClinicId(), id, medicine, dosage, defaultFrequency,
                    defaultDuration, defaultQuantity, defaultForm, defaultInstructions);
            redirectAttributes.addFlashAttribute("inventorySaved", "Inventory item updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("inventoryError", ex.getMessage());
        }
        return "redirect:/clinic/inventory";
    }

    @GetMapping("/clinic/inventory/template")
    public ResponseEntity<String> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"clinic-inventory-template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(inventoryService.templateCsv());
    }

    @GetMapping("/clinic/inventory/export")
    public ResponseEntity<String> export(@AuthenticationPrincipal AppUserPrincipal principal) {
        try {
            String csv = inventoryService.exportCsv(principal.getClinicId());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"clinic-inventory.csv\"")
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(csv);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/clinic/inventory/import")
    public String importCsv(@RequestParam("file") MultipartFile file,
                            @AuthenticationPrincipal AppUserPrincipal principal,
                            RedirectAttributes redirectAttributes) {
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("inventoryError", "Choose a CSV file to import.");
            return "redirect:/clinic/inventory";
        }
        try {
            int count = inventoryService.importCsv(principal.getClinicId(),
                    new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            redirectAttributes.addFlashAttribute("inventorySaved",
                    "Imported " + count + " inventory item(s).");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("inventoryError", ex.getMessage());
        } catch (java.io.IOException ex) {
            redirectAttributes.addFlashAttribute("inventoryError", "Could not read the CSV file.");
        }
        return "redirect:/clinic/inventory";
    }

    @PostMapping("/clinic/inventory/{id}/delete")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            inventoryService.delete(principal.getClinicId(), id);
            redirectAttributes.addFlashAttribute("inventorySaved", "Inventory item removed.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("inventoryError", ex.getMessage());
        }
        return "redirect:/clinic/inventory";
    }
}
