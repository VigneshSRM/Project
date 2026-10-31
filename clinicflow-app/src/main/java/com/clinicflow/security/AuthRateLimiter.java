package com.clinicflow.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Small in-process limiter for public authentication actions. A production
 * deployment should replace the store with a shared cache (for example Redis)
 * when more than one application instance is running.
 */
@Service
public class AuthRateLimiter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public boolean allow(String key, int max, Duration window) {
        Instant now = Instant.now();
        AtomicBoolean allowed = new AtomicBoolean(false);
        windows.compute(key, (ignored, existing) -> {
            if (existing == null || existing.resetAt.isBefore(now)) {
                allowed.set(true);
                return new Window(now.plus(window), 1);
            }
            if (existing.count >= max) {
                return existing;
            }
            existing.count++;
            allowed.set(true);
            return existing;
        });
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> entry.getValue().resetAt.isBefore(now));
        }
        return allowed.get();
    }

    private static final class Window {
        private final Instant resetAt;
        private int count;

        private Window(Instant resetAt, int count) {
            this.resetAt = resetAt;
            this.count = count;
        }
    }
}
