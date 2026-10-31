package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.Appointment;
import com.clinicflow.domain.enums.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientPatientIdOrderBySlotStartDesc(Long patientId);

    List<Appointment> findByClinicClinicIdAndSlotStartBetweenOrderBySlotStartAsc(
            Long clinicId, LocalDateTime from, LocalDateTime to);

    List<Appointment> findByDoctorDoctorIdAndSlotStartBetweenOrderBySlotStartAsc(
            Long doctorId, LocalDateTime from, LocalDateTime to);

    List<Appointment> findByClinicClinicIdOrderBySlotStartDesc(Long clinicId);

    List<Appointment> findByDoctorDoctorIdOrderBySlotStartDesc(Long doctorId);

    List<Appointment> findByDoctorDoctorIdAndSlotStartGreaterThanEqualOrderBySlotStartAsc(
            Long doctorId, LocalDateTime from);

    List<Appointment> findByDoctorDoctorIdAndStatusOrderBySlotStartAsc(
            Long doctorId, AppointmentStatus status);

    boolean existsByDoctorDoctorIdAndSlotStartAndStatusIn(
            Long doctorId, LocalDateTime slotStart, List<AppointmentStatus> statuses);

    @Query("""
            select a from Appointment a
            where a.doctor.doctorId = :doctorId and a.slotStart = :slotStart
              and a.status in :statuses
            """)
    List<Appointment> findActiveAtSlot(@Param("doctorId") Long doctorId,
                                       @Param("slotStart") LocalDateTime slotStart,
                                       @Param("statuses") List<AppointmentStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.appointmentId = :id")
    Optional<Appointment> findForUpdate(@Param("id") Long id);

    long countByStatus(AppointmentStatus status);

    long countByClinicClinicIdAndStatus(Long clinicId, AppointmentStatus status);

    long countByDoctorDoctorIdAndStatus(Long doctorId, AppointmentStatus status);

    long countBySlotStartBetween(LocalDateTime from, LocalDateTime to);

    long countByClinicClinicIdAndSlotStartBetween(Long clinicId, LocalDateTime from, LocalDateTime to);
}