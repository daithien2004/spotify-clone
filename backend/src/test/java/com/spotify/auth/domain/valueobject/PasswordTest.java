package com.spotify.auth.domain.valueobject;

import com.spotify.auth.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordTest {

    @Test
    void should_BeValid_when_AtLeast8Characters() {
        assertDoesNotThrow(() -> new Password("password123"));
    }

    @Test
    void should_ThrowException_when_TooShort() {
        assertThrows(DomainException.class, () -> new Password("1234567"));
    }

    @Test
    void should_ThrowException_when_ValueIsEmpty() {
        assertThrows(DomainException.class, () -> new Password(""));
        assertThrows(DomainException.class, () -> new Password(null));
    }

    @Test
    void should_ThrowException_when_OnlyLetters() {
        assertThrows(DomainException.class, () -> new Password("onlyletters"));
    }

    @Test
    void should_ThrowException_when_OnlyNumbers() {
        assertThrows(DomainException.class, () -> new Password("123456789"));
    }
}
