package com.clinicflow.domain.repository;

import com.clinicflow.domain.entity.AuthToken;
import com.clinicflow.domain.enums.AuthTokenPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByTokenHashAndPurpose(byte[] tokenHash, AuthTokenPurpose purpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AuthToken t where t.tokenHash = :tokenHash and t.purpose = :purpose")
    Optional<AuthToken> findForUpdate(@Param("tokenHash") byte[] tokenHash,
                                      @Param("purpose") AuthTokenPurpose purpose);

    @Modifying(flushAutomatically = true)
    @Query("delete from AuthToken t where t.user.userId = :userId and t.purpose = :purpose")
    void deleteByUserAndPurpose(@Param("userId") Long userId,
                                @Param("purpose") AuthTokenPurpose purpose);
}