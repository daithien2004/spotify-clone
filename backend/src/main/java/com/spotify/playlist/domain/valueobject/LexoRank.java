package com.spotify.playlist.domain.valueobject;

import java.util.Objects;

public record LexoRank(String value) {
    public LexoRank {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("LexoRank value cannot be null or empty");
        }
    }

    public int compareTo(LexoRank other) {
        return this.value.compareTo(other.value);
    }
}
