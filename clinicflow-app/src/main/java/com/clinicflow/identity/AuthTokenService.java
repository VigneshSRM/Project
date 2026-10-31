package com.clinicflow.identity;

import com.clinicflow.domain.entity.AuthToken;
import com.clinicflow.domain.entity.User;
import com.clinicflow.domain.enums.AuthTokenPurpose;
import com.clinicflow.domain.repository.AuthTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class AuthTokenService {

    private final AuthTokenRepository repository;
    private final Duration verifyEmailTtl;
    private final Duration resetPasswordTtl;
    private final Duration staffInviteTtl;
    private final Duration emailChangeTtl;

    public AuthTokenService(AuthTokenRepository repository,
                            @Value("${clinicflow.auth.verify-email-ttl-hours:24}") long verifyEmailTtlHours,
                            @Value("${clinicflow.auth.reset-password-ttl-minutes:60}") long resetPasswordTtlMinutes,
                            @Value("${clinicflow.auth.staff-invite-ttl-hours:72}") long staffInviteTtlHours,
                            @Value("${clinicflow.auth.email-change-ttl-hours:24}") long emailChangeTtlHours) {
        this.repository = repository;
        this.verifyEmailTtl = Duration.ofHours(verifyEmailTtlHours);
        this.resetPasswordTtl = Duration.ofMinutes(resetPasswordTtlMinutes);
        this.staffInviteTtl = Duration.ofHours(staffInviteTtlHours);
        this.emailChangeTtl = Duration.ofHours(emailChangeTtlHours);
    }

    /**
     * Issues one active token per user/purpose. Older tokens are deleted before the
     * new token is stored, so a resend invalidates the previous link.
     */
    @Transactional
    public IssuedToken issue(User user, AuthTokenPurpose purpose) {
        repository.deleteByUserAndPurpose(user.getUserId(), purpose);

        String rawToken = TokenHasher.newToken();
        AuthToken token = new AuthToken();
        token.setUser(user);
        token.setPurpose(purpose);
        token.setTokenHash(TokenHasher.sha256(rawToken));
        token.setExpiresAt(now().plus(ttlFor(purpose)));
        token.setCreatedAt(now());
        repository.save(token);
        return new IssuedToken(rawToken, token);
    }

    /**
     * Locks the row, validates it and marks it used. Callers must be transactional
     * so the mark-used and the domain change commit together.
     */
    @Transactional
    public Optional<AuthToken> consume(String rawToken, AuthTokenPurpose purpose) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        LocalDateTime now = now();
        return repository.findForUpdate(TokenHasher.sha256(rawToken), purpose)
                .filter(token -> token.getUsedAt() == null)
                .filter(token -> token.getExpiresAt().isAfter(now))
                .map(token -> {
                    token.setUsedAt(now);
                    return token;
                });
    }

    @Transactional(readOnly = true)
    public boolean isValid(String rawToken, AuthTokenPurpose purpose) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        LocalDateTime now = now();
        return repository.findByTokenHashAndPurpose(TokenHasher.sha256(rawToken), purpose)
                .filter(token -> token.getUsedAt() == null)
                .filter(token -> token.getExpiresAt().isAfter(now))
                .isPresent();
    }

    public Duration ttlFor(AuthTokenPurpose purpose) {
        return switch (purpose) {
            case VERIFY_EMAIL -> verifyEmailTtl;
            case RESET_PASSWORD -> resetPasswordTtl;
            case STAFF_INVITE -> staffInviteTtl;
            case EMAIL_CHANGE -> emailChangeTtl;
        };
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
