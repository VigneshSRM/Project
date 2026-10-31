package com.clinicflow.identity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    @Test
    void acceptsPolicyCompliantPasswords() {
        assertThat(PasswordPolicy.isValid("ClinicFlow1")).isTrue();
        assertThat(PasswordPolicy.isValid("a123456789")).isTrue();
    }

    @Test
    void rejectsWeakOrMissingPasswords() {
        assertThat(PasswordPolicy.isValid(null)).isFalse();
        assertThat(PasswordPolicy.isValid("short1")).isFalse();
        assertThat(PasswordPolicy.isValid("onlyletters")).isFalse();
        assertThat(PasswordPolicy.isValid("1234567890")).isFalse();
        assertThat(PasswordPolicy.isValid("a".repeat(129))).isFalse();
    }
}
