package com.clinicflow.domain.entity;

import com.clinicflow.domain.enums.PharmacyUserRole;
import com.clinicflow.domain.enums.PharmacyUserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "PharmacyUsers")
@Getter
@Setter
@NoArgsConstructor
public class PharmacyUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pharmacy_user_id")
    private Long pharmacyUserId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PharmacyUserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private PharmacyUserStatus status = PharmacyUserStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}