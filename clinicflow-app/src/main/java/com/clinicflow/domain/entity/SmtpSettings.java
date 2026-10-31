package com.clinicflow.domain.entity;

import com.clinicflow.domain.enums.SmtpSecurity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "SmtpSettings")
@Getter
@Setter
@NoArgsConstructor
public class SmtpSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settings_id")
    private Long settingsId;

    @Column(nullable = false, length = 200)
    private String host;

    @Column(nullable = false)
    private Integer port;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SmtpSecurity security = SmtpSecurity.STARTTLS;

    @Column(length = 200)
    private String username;

    @Column(name = "password_encrypted", columnDefinition = "varbinary(512)")
    private byte[] passwordEncrypted;

    @Column(name = "from_name", length = 200)
    private String fromName;

    @Column(name = "from_address", length = 254)
    private String fromAddress;

    @Column(nullable = false)
    private Boolean active = false;

    @Column(name = "last_test_at")
    private LocalDateTime lastTestAt;

    @Column(name = "last_test_result", length = 200)
    private String lastTestResult;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}