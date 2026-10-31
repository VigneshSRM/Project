package com.clinicflow.web.patient;

import com.clinicflow.security.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.UUID;

@Controller
public class PatientBookingController {

    private final ClinicDiscoveryService discoveryService;
    private final SlotService slotService;
    private final BookingService bookingService;

    public PatientBookingController(ClinicDiscoveryService discoveryService, SlotService slotService,
                                    BookingService bookingService) {
        this.discoveryService = discoveryService;
        this.slotService = slotService;
        this.bookingService = bookingService;
    }

    @GetMapping("/patient/clinics")
    public String clinics(@RequestParam(required = false) String q,
                          @RequestParam(required = false) Double lat,
                          @RequestParam(required = false) Double lng,
                          Model model) {
        model.addAttribute("clinics", discoveryService.search(q, lat, lng));
        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("lat", lat);
        model.addAttribute("lng", lng);
        model.addAttribute("pageTitle", "Find Nearby Clinics");
        return "patient/clinics";
    }

    @GetMapping("/patient/clinics/{uuid}")
    public String clinic(@PathVariable UUID uuid, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("clinic", discoveryService.detail(uuid));
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("clinicError", ex.getMessage());
            return "redirect:/patient/clinics";
        }
        model.addAttribute("pageTitle", "Clinic");
        return "patient/clinic-detail";
    }

    @GetMapping("/patient/book")
    public String bookForm(@RequestParam(required = false) Long clinicId,
                           @RequestParam(required = false) Long doctorId,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           Model model, RedirectAttributes redirectAttributes) {
        if (clinicId == null) {
            return "redirect:/patient/clinics";
        }
        ClinicDiscoveryService.ClinicProfile profile;
        try {
            profile = discoveryService.profileById(clinicId);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("clinicError", ex.getMessage());
            return "redirect:/patient/clinics";
        }
        if (profile.doctors().isEmpty()) {
            model.addAttribute("bookingError", "This clinic has no active doctors available for booking.");
            model.addAttribute("clinic", profile);
            model.addAttribute("pageTitle", "Book appointment");
            return "patient/booking";
        }
        Long selectedDoctor = doctorId == null ? profile.doctors().get(0).id() : doctorId;
        final Long doctorForSlots = selectedDoctor;
        var doctor = profile.doctors().stream().filter(option -> option.id().equals(doctorForSlots))
                .findFirst().orElse(profile.doctors().get(0));
        LocalDate selectedDate = date == null || date.isBefore(LocalDate.now())
                ? LocalDate.now().plusDays(1) : date;
        var slots = slotService.slots(profile.id(), doctor.id(), selectedDate);
        if (!model.containsAttribute("bookingForm")) {
            BookingForm form = new BookingForm();
            form.setClinicId(profile.id());
            form.setDoctorId(doctor.id());
            model.addAttribute("bookingForm", form);
        }
        model.addAttribute("clinic", profile);
        model.addAttribute("doctor", doctor);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("slots", slots);
        model.addAttribute("pageTitle", "Book appointment");
        return "patient/booking";
    }

    @PostMapping("/patient/book")
    public String book(@Valid @org.springframework.web.bind.annotation.ModelAttribute("bookingForm") BookingForm form,
                       BindingResult bindingResult,
                       @AuthenticationPrincipal AppUserPrincipal principal,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("bookingError", "Choose a slot before confirming.");
            return redirectBack(form);
        }
        try {
            Long id = bookingService.book(principal.getUserId(), form);
            redirectAttributes.addFlashAttribute("appointmentSaved",
                    "Your appointment is booked. A confirmation email has been queued.");
            return "redirect:/patient/appointments/" + id;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("bookingError", ex.getMessage());
            return redirectBack(form);
        }
    }

    @GetMapping("/patient/appointments")
    public String appointments(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("appointments", bookingService.list(principal.getUserId()));
        model.addAttribute("pageTitle", "Appointments");
        return "patient/appointments";
    }

    @GetMapping("/patient/appointments/{id}")
    public String appointment(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal principal,
                              Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("appointment", bookingService.detail(principal.getUserId(), id,
                    principal.getUserId()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("appointmentError", ex.getMessage());
            return "redirect:/patient/appointments";
        }
        model.addAttribute("pageTitle", "Appointment");
        return "patient/appointment-detail";
    }

    @PostMapping("/patient/appointments/{id}/cancel")
    public String cancel(@PathVariable Long id, @RequestParam(required = false) String reason,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancel(principal.getUserId(), id, reason, principal.getUserId());
            redirectAttributes.addFlashAttribute("appointmentSaved", "Appointment cancelled.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("appointmentError", ex.getMessage());
        }
        return "redirect:/patient/appointments/" + id;
    }

    @GetMapping("/patient/appointments/{id}/reschedule")
    public String rescheduleForm(@PathVariable Long id,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 @AuthenticationPrincipal AppUserPrincipal principal,
                                 Model model, RedirectAttributes redirectAttributes) {
        BookingService.PatientAppointmentDetail detail;
        try {
            detail = bookingService.detail(principal.getUserId(), id, principal.getUserId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("appointmentError", ex.getMessage());
            return "redirect:/patient/appointments";
        }
        if (!detail.canChange()) {
            redirectAttributes.addFlashAttribute("appointmentError",
                    "This appointment can no longer be rescheduled online.");
            return "redirect:/patient/appointments/" + id;
        }
        LocalDate selectedDate = date == null || date.isBefore(LocalDate.now())
                ? LocalDate.now().plusDays(1) : date;
        var slots = slotService.slots(detail.clinicId(), detail.doctorId(), selectedDate);
        BookingForm form = new BookingForm();
        form.setClinicId(detail.clinicId());
        form.setDoctorId(detail.doctorId());
        model.addAttribute("rescheduleForm", form);
        model.addAttribute("appointment", detail);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("slots", slots);
        model.addAttribute("pageTitle", "Reschedule appointment");
        return "patient/reschedule";
    }

    @PostMapping("/patient/appointments/{id}/reschedule")
    public String reschedule(@PathVariable Long id,
                             @Valid @org.springframework.web.bind.annotation.ModelAttribute("rescheduleForm") BookingForm form,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal AppUserPrincipal principal,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("appointmentError", "Choose a new slot.");
            return "redirect:/patient/appointments/" + id + "/reschedule";
        }
        try {
            bookingService.reschedule(principal.getUserId(), id, form, principal.getUserId());
            redirectAttributes.addFlashAttribute("appointmentSaved", "Appointment rescheduled.");
            return "redirect:/patient/appointments/" + id;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("appointmentError", ex.getMessage());
            return "redirect:/patient/appointments/" + id + "/reschedule";
        }
    }

    private static String redirectBack(BookingForm form) {
        LocalDate date = form.getSlotStart() == null ? LocalDate.now().plusDays(1)
                : form.getSlotStart().toLocalDate();
        return "redirect:/patient/book?clinicId=" + form.getClinicId()
                + "&doctorId=" + form.getDoctorId() + "&date=" + date;
    }
}
