package com.clinicflow.common;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the caller address without trusting arbitrary client headers beyond the
 * first proxy hop. Deployments behind a trusted reverse proxy should overwrite
 * X-Forwarded-For at the edge.
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return truncate(first, 45);
            }
        }
        return truncate(request.getRemoteAddr(), 45);
    }

    public static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
