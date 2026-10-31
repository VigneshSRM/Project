package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.EmailLog;
import com.clinicflow.domain.enums.EmailStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    Optional<EmailLog> findByIdempotencyKey(String idempotencyKey);

    List<EmailLog> findTop50ByOrderByCreatedAtDesc();

    List<EmailLog> findByStatusOrderByCreatedAtAsc(EmailStatus status);

    long countByStatus(EmailStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e from EmailLog e
            where e.status = :status
              and (e.nextAttemptAt is null or e.nextAttemptAt <= :now)
            order by e.createdAt asc
            """)
    List<EmailLog> findDueForUpdate(@Param("status") EmailStatus status,
                                    @Param("now") LocalDateTime now,
                                    Pageable pageable);
}