package com.clinicflow.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ConsultationAddenda")
@Getter
@Setter
@NoArgsConstructor
public class ConsultationAddendum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "addendum_id")
    private Long addendumId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultation_id", nullable = false)
    private Consultation consultation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_user_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 300)
    private String reason;

    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String text;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}