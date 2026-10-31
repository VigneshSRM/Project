package com.clinicflow.identity;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TokenHasherTest {

    @Test
    void generatesUrlSafeHighEntropyTokens() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            String token = TokenHasher.newToken();
            assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]{43}");
            tokens.add(token);
        }
        assertThat(tokens).hasSize(1_000);
    }

    @Test
    void hashesDeterministicallyWithoutStoringTheRawToken() {
        String token = "example-token";
        assertThat(TokenHasher.sha256(token)).hasSize(32);
        assertThat(TokenHasher.sha256(token)).isEqualTo(TokenHasher.sha256(token));
        assertThat(TokenHasher.sha256Hex(token)).hasSize(64).isNotEqualTo(token);
    }
}
