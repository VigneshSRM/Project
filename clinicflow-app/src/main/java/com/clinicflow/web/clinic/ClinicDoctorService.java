package com.clinicflow.web.clinic;

import com.clinicflow.audit.AuditService;
import com.clinicflow.domain.entity.Clinic;
import com.clinicflow.domain.entity.ClinicDoctor;
import com.clinicflow.domain.entity.Doctor;
import com.clinicflow.domain.entity.DoctorSchedule;
import com.clinicflow.domain.entity.User;
import com.clinicflow.domain.enums.AuditOutcome;
import com.clinicflow.domain.enums.DoctorStatus;
import com.clinicflow.domain.enums.Role;
import com.clinicflow.domain.enums.UserStatus;
import com.clinicflow.domain.repository.ClinicDoctorRepository;
import com.clinicflow.domain.repository.ClinicRepository;
import com.clinicflow.domain.repository.DoctorRepository;
import com.clinicflow.domain.repository.DoctorScheduleRepository;
import com.clinicflow.domain.repository.DoctorUserLinkRepository;
import com.clinicflow.identity.InvitationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ClinicDoctorService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;
    private final ClinicDoctorRepository clinicDoctorRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final DoctorUserLinkRepository doctorUserLinkRepository;
    private final InvitationService invitationService;
    private final AuditService auditService;

    public ClinicDoctorService(ClinicRepository clinicRepository, DoctorRepository doctorRepository,
                               ClinicDoctorRepository clinicDoctorRepository,
                               DoctorScheduleRepository scheduleRepository,
                               DoctorUserLinkRepository doctorUserLinkRepository,
                               InvitationService invitationService,
                               AuditService auditService) {
        this.clinicRepository = clinicRepository;
        this.doctorRepository = doctorRepository;
        this.clinicDoctorRepository = clinicDoctorRepository;
        this.scheduleRepository = scheduleRepository;
        this.doctorUserLinkRepository = doctorUserLinkRepository;
        this.invitationService = invitationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<DoctorRow> list(Long clinicId) {
        return clinicDoctorRepository.findByClinicClinicId(clinicId).stream()
                .map(this::toRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public DoctorDetail detail(Long clinicId, Long doctorId) {
        ClinicDoctor link = clinicDoctorRepository.findByClinicClinicIdAndDoctorDoctorId(clinicId, doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor is not part of this clinic."));
        User account = doctorUserLinkRepository.findByDoctorDoctorId(doctorId)
                .map(link1 -> link1.getUser()).orElse(null);
        List<DoctorSchedule> schedules = scheduleRepository
                .findByDoctorDoctorIdAndClinicClinicId(doctorId, clinicId);
        return new DoctorDetail(link.getDoctor().getDoctorId(), link.getDoctor().getName(),
                link.getDoctor().getSpecialization(), link.getFee(), link.getStatus(),
                account == null ? null : account.getEmail(),
                account == null ? null : account.getStatus(),
                account == null ? null : account.getUserId(), schedules);
    }

    @Transactional
    public Long create(Long clinicId, DoctorForm form, Long actorUserId) {
        Clinic clinic = clinicRepository.findById(clinicId).orElseThrow();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Doctor doctor = new Doctor();
        doctor.setName(form.getName().trim());
        doctor.setSpecialization(trimToNull(form.getSpecialization()));
        doctor.setStatus(form.getStatus());
        doctor.setCreatedAt(now);
        doctor.setUpdatedAt(now);
        doctor = doctorRepository.saveAndFlush(doctor);

        ClinicDoctor link = new ClinicDoctor();
        link.setClinic(clinic);
        link.setDoctor(doctor);
        link.setFee(form.getFee());
        link.setStatus(form.getStatus());
        link.setCreatedAt(now);
        clinicDoctorRepository.save(link);

        if (form.getEmail() != null && !form.getEmail().isBlank()) {
            invitationService.inviteDoctor(form.getEmail(), doctor, clinic, clinic.getName(), actorUserId);
        }
        auditService.record(actorUserId, Role.CLINIC_ADMIN, "DOCTOR_CREATED", "Doctor",
                String.valueOf(doctor.getDoctorId()), null, AuditOutcome.SUCCESS);
        return doctor.getDoctorId();
    }

    @Transactional
    public void update(Long clinicId, Long doctorId, DoctorForm form, Long actorUserId) {
        ClinicDoctor link = clinicDoctorRepository.findByClinicClinicIdAndDoctorDoctorId(clinicId, doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor is not part of this clinic."));
        Doctor doctor = link.getDoctor();
        doctor.setName(form.getName().trim());
        doctor.setSpecialization(trimToNull(form.getSpecialization()));
        doctor.setStatus(form.getStatus());
        doctor.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        doctorRepository.save(doctor);
        link.setFee(form.getFee());
        link.setStatus(form.getStatus());
        clinicDoctorRepository.save(link);
        auditService.record(actorUserId, Role.CLINIC_ADMIN, "DOCTOR_UPDATED", "Doctor",
                String.valueOf(doctorId), null, AuditOutcome.SUCCESS);
    }

    @Transactional(readOnly = true)
    public DoctorForm formFor(Long clinicId, Long doctorId) {
        ClinicDoctor link = clinicDoctorRepository.findByClinicClinicIdAndDoctorDoctorId(clinicId, doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor is not part of this clinic."));
        DoctorForm form = new DoctorForm();
        form.setName(link.getDoctor().getName());
        form.setSpecialization(link.getDoctor().getSpecialization());
        form.setFee(link.getFee());
        form.setStatus(link.getStatus());
        form.setEmail(null);
        return form;
    }

    @Transactional
    private static String weekdayName(int weekday) {
        return switch (weekday) {
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            case 6 -> "Saturday";
            case 7 -> "Sunday";
            default -> "day " + weekday;
        };
    }

    public void addSchedule(Long clinicId, Long doctorId, ScheduleForm form, Long actorUserId) {
        ClinicDoctor link = clinicDoctorRepository.findByClinicClinicIdAndDoctorDoctorId(clinicId, doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor is not part of this clinic."));
        if (!form.getEndTime().isAfter(form.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        if (form.getEffectiveFrom() != null && form.getEffectiveTo() != null
                && form.getEffectiveTo().isBefore(form.getEffectiveFrom())) {
            throw new IllegalArgumentException("Effective-to date cannot be before effective-from.");
        }
        List<Integer> weekdays = form.getWeekdays() == null ? List.of()
                : new ArrayList<>(new LinkedHashSet<>(form.getWeekdays()));
        if (weekdays.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one weekday.");
        }
        for (Integer weekday : weekdays) {
            if (weekday == null || weekday < 1 || weekday > 7) {
                throw new IllegalArgumentException("Choose a valid weekday.");
            }
            List<DoctorSchedule> sameDay = scheduleRepository
                    .findByDoctorDoctorIdAndClinicClinicIdAndWeekday(doctorId, clinicId, weekday);
            for (DoctorSchedule existing : sameDay) {
                boolean overlaps = form.getStartTime().isBefore(existing.getEndTime())
                        && form.getEndTime().isAfter(existing.getStartTime());
                boolean dateOverlaps = (form.getEffectiveFrom() == null || existing.getEffectiveTo() == null
                        || !form.getEffectiveFrom().isAfter(existing.getEffectiveTo()))
                        && (form.getEffectiveTo() == null || existing.getEffectiveFrom() == null
                        || !form.getEffectiveTo().isBefore(existing.getEffectiveFrom()));
                if (overlaps && dateOverlaps) {
                    throw new IllegalArgumentException(
                            "This schedule overlaps an existing schedule on " + weekdayName(weekday) + ".");
                }
            }
            DoctorSchedule schedule = new DoctorSchedule();
            schedule.setDoctor(link.getDoctor());
            schedule.setClinic(link.getClinic());
            schedule.setWeekday(weekday);
            schedule.setStartTime(form.getStartTime());
            schedule.setEndTime(form.getEndTime());
            schedule.setSlotMinutes(form.getSlotMinutes());
            schedule.setEffectiveFrom(form.getEffectiveFrom());
            schedule.setEffectiveTo(form.getEffectiveTo());
            schedule.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            scheduleRepository.save(schedule);
            auditService.record(actorUserId, Role.CLINIC_ADMIN, "DOCTOR_SCHEDULE_CREATED", "DoctorSchedule",
                    String.valueOf(schedule.getScheduleId()), null, AuditOutcome.SUCCESS);
        }
    }

    @Transactional
    public void deleteSchedule(Long clinicId, Long doctorId, Long scheduleId, Long actorUserId) {
        DoctorSchedule schedule = scheduleRepository.findById(scheduleId)
                .filter(item -> item.getClinic().getClinicId().equals(clinicId))
                .filter(item -> item.getDoctor().getDoctorId().equals(doctorId))
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found."));
        scheduleRepository.delete(schedule);
        auditService.record(actorUserId, Role.CLINIC_ADMIN, "DOCTOR_SCHEDULE_DELETED", "DoctorSchedule",
                String.valueOf(scheduleId), null, AuditOutcome.SUCCESS);
    }

    @Transactional
    public boolean resendInvite(Long clinicId, Long doctorId, Long actorUserId) {
        var link = doctorUserLinkRepository.findByDoctorDoctorId(doctorId).orElse(null);
        if (link == null || link.getUser().getStatus() != UserStatus.PENDING) {
            return false;
        }
        Clinic clinic = clinicRepository.findById(clinicId).orElseThrow();
        invitationService.resend(link.getUser(), clinic.getName(), "Doctor", actorUserId);
        return true;
    }

    private DoctorRow toRow(ClinicDoctor link) {
        Doctor doctor = link.getDoctor();
        User account = doctorUserLinkRepository.findByDoctorDoctorId(doctor.getDoctorId())
                .map(link1 -> link1.getUser()).orElse(null);
        long schedules = scheduleRepository.findByDoctorDoctorIdAndClinicClinicId(
                doctor.getDoctorId(), link.getClinic().getClinicId()).size();
        return new DoctorRow(doctor.getDoctorId(), doctor.getName(), doctor.getSpecialization(),
                link.getFee(), link.getStatus(), schedules,
                account == null ? "—" : account.getEmail(),
                account == null ? "No login" : account.getStatus().name());
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record DoctorRow(Long id, String name, String specialization, java.math.BigDecimal fee,
                            DoctorStatus status, long schedules, String accountEmail, String accountStatus) {
    }

    public record DoctorDetail(Long id, String name, String specialization, java.math.BigDecimal fee,
                               DoctorStatus status, String accountEmail, UserStatus accountStatus,
                               Long accountUserId, List<DoctorSchedule> schedules) {
        public String weekdayLabel(int weekday) {
            return switch (weekday) {
                case 1 -> "Monday";
                case 2 -> "Tuesday";
                case 3 -> "Wednesday";
                case 4 -> "Thursday";
                case 5 -> "Friday";
                case 6 -> "Saturday";
                case 7 -> "Sunday";
                default -> "?";
            };
        }

        public String time(DoctorSchedule schedule) {
            return TIME.format(schedule.getStartTime()) + "–" + TIME.format(schedule.getEndTime());
        }
    }
}
