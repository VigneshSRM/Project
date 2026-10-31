package com.clinicflow.identity;

import com.clinicflow.domain.entity.AuthToken;

public record IssuedToken(String rawToken, AuthToken entity) {
}
