package com.clinicflow.domain.entity;

import com.clinicflow.domain.enums.Gender;
import com.clinicflow.domain.enums.PatientSource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Patients")
@Getter
@Setter
@NoArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "public_patient_id", nullable = false, unique = true, length = 20)
    private String publicPatientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false)
    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 30)
    private String mobile;

    @Column(length = 254)
    private String email;

    @Column(name = "address_line1", length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Column(length = 1000)
    private String allergies;

    @Column(length = 1000)
    private String conditions;

    @Column(name = "emergency_name", length = 200)
    private String emergencyName;

    @Column(name = "emergency_phone", length = 30)
    private String emergencyPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_flag", nullable = false, length = 20)
    private PatientSource sourceFlag = PatientSource.CLINIC_ENTERED;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_into_patient_id")
    private Patient mergedInto;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Transient
    public String getDisplayName() {
        return firstName + " " + lastName;
    }
}