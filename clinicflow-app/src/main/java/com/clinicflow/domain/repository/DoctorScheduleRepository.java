package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    List<DoctorSchedule> findByDoctorDoctorIdAndClinicClinicId(Long doctorId, Long clinicId);

    List<DoctorSchedule> findByClinicClinicId(Long clinicId);

    List<DoctorSchedule> findByDoctorDoctorIdAndClinicClinicIdAndWeekday(
            Long doctorId, Long clinicId, Integer weekday);
}