package com.clinicflow.web.patient;

import com.clinicflow.domain.entity.Appointment;
import com.clinicflow.domain.entity.DoctorSchedule;
import com.clinicflow.domain.enums.AppointmentStatus;
import com.clinicflow.domain.enums.ClinicStatus;
import com.clinicflow.domain.enums.DoctorStatus;
import com.clinicflow.domain.repository.AppointmentRepository;
import com.clinicflow.domain.repository.ClinicDoctorRepository;
import com.clinicflow.domain.repository.ClinicRepository;
import com.clinicflow.domain.repository.DoctorRepository;
import com.clinicflow.domain.repository.DoctorScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
public class SlotService {

    public static final List<AppointmentStatus> ACTIVE_STATUSES = List.of(
            AppointmentStatus.BOOKED, AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN,
            AppointmentStatus.IN_CONSULTATION);

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ClinicRepository clinicRepository;
    private final ClinicDoctorRepository clinicDoctorRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;

    public SlotService(ClinicRepository clinicRepository, ClinicDoctorRepository clinicDoctorRepository,
                       DoctorRepository doctorRepository, DoctorScheduleRepository scheduleRepository,
                       AppointmentRepository appointmentRepository) {
        this.clinicRepository = clinicRepository;
        this.clinicDoctorRepository = clinicDoctorRepository;
        this.doctorRepository = doctorRepository;
        this.scheduleRepository = scheduleRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public DaySlots slots(Long clinicId, Long doctorId, LocalDate date) {
        if (clinicId == null || doctorId == null || date == null) {
            return new DaySlots(date, doctorId, clinicId, List.of(), 0);
        }
        var clinic = clinicRepository.findById(clinicId)
                .filter(candidate -> candidate.getStatus() == ClinicStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Clinic is not available."));
        var doctor = doctorRepository.findById(doctorId)
                .filter(candidate -> candidate.getStatus() == DoctorStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Doctor is not available."));
        var link = clinicDoctorRepository.findByClinicClinicIdAndDoctorDoctorId(clinicId, doctorId)
                .filter(candidate -> candidate.getStatus() == DoctorStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Doctor is not available at this clinic."));

        int weekday = date.getDayOfWeek().getValue();
        List<DoctorSchedule> schedules = scheduleRepository
                .findByDoctorDoctorIdAndClinicClinicIdAndWeekday(doctorId, clinicId, weekday).stream()
                .filter(schedule -> schedule.getEffectiveFrom() == null
                        || !date.isBefore(schedule.getEffectiveFrom()))
                .filter(schedule -> schedule.getEffectiveTo() == null
                        || !date.isAfter(schedule.getEffectiveTo()))
                .toList();

        List<Appointment> booked = appointmentRepository
                .findByDoctorDoctorIdAndSlotStartBetweenOrderBySlotStartAsc(doctorId,
                        date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        Set<LocalDateTime> taken = new HashSet<>();
        for (Appointment appointment : booked) {
            if (ACTIVE_STATUSES.contains(appointment.getStatus())) {
                taken.add(appointment.getSlotStart());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        TreeSet<LocalDateTime> candidates = new TreeSet<>();
        for (DoctorSchedule schedule : schedules) {
            LocalTime cursor = schedule.getStartTime();
            while (!cursor.plusMinutes(schedule.getSlotMinutes()).isAfter(schedule.getEndTime())) {
                LocalDateTime slot = LocalDateTime.of(date, cursor);
                if (slot.isAfter(now) && !taken.contains(slot)) {
                    candidates.add(slot);
                }
                cursor = cursor.plusMinutes(schedule.getSlotMinutes());
            }
        }
        List<SlotOption> result = new ArrayList<>();
        for (LocalDateTime slot : candidates) {
            result.add(new SlotOption(slot, TIME.format(slot)));
        }
        return new DaySlots(date, doctorId, clinicId, result, schedules.size());
    }

    @Transactional(readOnly = true)
    public boolean isValidSlot(Long clinicId, Long doctorId, LocalDateTime slot) {
        if (slot == null) {
            return false;
        }
        return slots(clinicId, doctorId, slot.toLocalDate()).slots().stream()
                .anyMatch(option -> option.start().equals(slot));
    }

    @Transactional(readOnly = true)
    public int slotLengthMinutes(Long clinicId, Long doctorId, LocalDateTime slot) {
        if (slot == null) {
            return 15;
        }
        int weekday = slot.getDayOfWeek().getValue();
        for (DoctorSchedule schedule : scheduleRepository
                .findByDoctorDoctorIdAndClinicClinicIdAndWeekday(doctorId, clinicId, weekday)) {
            if (!slot.toLocalTime().isBefore(schedule.getStartTime())
                    && slot.toLocalTime().isBefore(schedule.getEndTime())) {
                return schedule.getSlotMinutes();
            }
        }
        return 15;
    }

    public record SlotOption(LocalDateTime start, String label) {
    }

    public record DaySlots(LocalDate date, Long doctorId, Long clinicId, List<SlotOption> slots,
                           long scheduleCount) {
    }
}
