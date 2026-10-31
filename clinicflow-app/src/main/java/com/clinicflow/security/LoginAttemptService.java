package com.clinicflow.security;

import com.clinicflow.common.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary login throttling after repeated failures. The key is IP + email so a
 * single noisy client cannot lock an unrelated user out from another address.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public String key(HttpServletRequest request, String email) {
        String address = ClientIp.resolve(request);
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return (address == null ? "unknown" : address) + "|" + normalizedEmail;
    }

    public boolean isBlocked(String key) {
        Window window = windows.get(key);
        if (window == null) {
            return false;
        }
        if (window.resetAt.isBefore(Instant.now())) {
            windows.remove(key, window);
            return false;
        }
        return window.failures >= MAX_ATTEMPTS;
    }

    public void onFailure(String key) {
        Instant now = Instant.now();
        windows.compute(key, (ignored, existing) -> {
            if (existing == null || existing.resetAt.isBefore(now)) {
                return new Window(now.plus(WINDOW), 1);
            }
            existing.failures++;
            return existing;
        });
    }

    public void onSuccess(String key) {
        windows.remove(key);
    }

    private static final class Window {
        private final Instant resetAt;
        private int failures;

        private Window(Instant resetAt, int failures) {
            this.resetAt = resetAt;
            this.failures = failures;
        }
    }
}
