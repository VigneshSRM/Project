package com.clinicflow.common;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PublicIdGeneratorTest {

    @Test
    void prescriptionCodeIs32MixedAlphanumericAndUnique() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            String code = PublicIdGenerator.prescriptionCode();
            assertThat(code).hasSize(32).matches("[A-Za-z0-9]{32}");
            codes.add(code);
        }
        assertThat(codes).hasSize(1_000);
    }

    @Test
    void publicIdsAreReadable() {
        assertThat(PublicIdGenerator.patientId(7)).matches("PAT-\\d{4}-0007");
        assertThat(PublicIdGenerator.appointmentId(42)).matches("APT-\\d{4}-00042");
    }
}