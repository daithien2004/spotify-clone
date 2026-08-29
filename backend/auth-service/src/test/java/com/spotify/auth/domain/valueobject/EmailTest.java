package com.spotify.auth.domain.valueobject;

import com.spotify.auth.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @Test
    void should_BeValid_when_FormatIsCorrect() {
        assertDoesNotThrow(() -> new Email("test@example.com"));
    }

    @Test
    void should_ThrowException_when_FormatIsInvalid() {
        assertThrows(DomainException.class, () -> new Email("invalid-email"));
        assertThrows(DomainException.class, () -> new Email("test@"));
        assertThrows(DomainException.class, () -> new Email("@example.com"));
    }

    @Test
    void should_ThrowException_when_ValueIsEmpty() {
        assertThrows(DomainException.class, () -> new Email(""));
        assertThrows(DomainException.class, () -> new Email(null));
    }
}
