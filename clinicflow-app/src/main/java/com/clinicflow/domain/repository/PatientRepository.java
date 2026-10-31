package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUserUserId(Long userId);

    Optional<Patient> findByPublicPatientId(String publicPatientId);

    List<Patient> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);

    List<Patient> findAllByOrderByCreatedAtDesc();

    @Query("""
            select p from Patient p
            where p.mergedInto is null
              and (:q is null
                   or lower(p.firstName) like lower(concat('%', :q, '%'))
                   or lower(p.lastName) like lower(concat('%', :q, '%'))
                   or lower(concat(p.firstName, ' ', p.lastName)) like lower(concat('%', :q, '%'))
                   or lower(p.publicPatientId) like lower(concat('%', :q, '%'))
                   or lower(coalesce(p.email, '')) like lower(concat('%', :q, '%'))
                   or coalesce(p.mobile, '') like concat('%', :q, '%'))
            order by p.createdAt desc
            """)
    List<Patient> searchMasters(@Param("q") String query);

    @Query("""
            select p from Patient p join ClinicPatient cp on cp.patient = p
            where cp.clinic.clinicId = :clinicId and p.mergedInto is null
              and (:q is null
                   or lower(p.firstName) like lower(concat('%', :q, '%'))
                   or lower(p.lastName) like lower(concat('%', :q, '%'))
                   or lower(concat(p.firstName, ' ', p.lastName)) like lower(concat('%', :q, '%'))
                   or lower(p.publicPatientId) like lower(concat('%', :q, '%'))
                   or lower(coalesce(p.email, '')) like lower(concat('%', :q, '%'))
                   or coalesce(p.mobile, '') like concat('%', :q, '%'))
            order by p.createdAt desc
            """)
    List<Patient> searchClinicPatients(@Param("clinicId") Long clinicId, @Param("q") String query);

    List<Patient> findByMergedIntoIsNullOrderByCreatedAtDesc();

    List<Patient> findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDobAndMergedIntoIsNull(
            String firstName, String lastName, LocalDate dob);
}