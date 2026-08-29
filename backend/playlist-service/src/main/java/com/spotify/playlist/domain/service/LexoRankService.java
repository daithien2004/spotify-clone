package com.spotify.playlist.domain.service;

import com.spotify.playlist.domain.valueobject.LexoRank;

/**
 * Domain service — pure rank arithmetic over the 63-character rank alphabet
 * (0-9, A-Z, _, a-z) used by the standard lexorank algorithm. No Spring imports
 * (Clean Architecture hard-gate). Registered as a Spring bean via
 * {@code PlaylistInfrastructureConfig}.
 */
public class LexoRankService {

    /** Rank universe: digits < uppercase < underscore < lowercase (matches byte order). */
    private static final String RANK_CHARS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz";
    private static final char MIN_CHAR = '0';
    private static final char MAX_CHAR = 'z';

    /** Rank length after which there is too little room for inserts — triggers rebalance. */
    public static final int PRECISION_LIMIT = 30;

    public LexoRank getInitialRank() {
        return new LexoRank("m");
    }

    public LexoRank calculateMid(LexoRank prev, LexoRank next) {
        if (prev == null && next == null) {
            return getInitialRank();
        }

        String prevVal = (prev == null) ? String.valueOf(MIN_CHAR) : prev.value();
        String nextVal = (next == null) ? String.valueOf(MAX_CHAR) : next.value();

        if (prevVal.compareTo(nextVal) >= 0) {
            throw new IllegalArgumentException("prev must be strictly less than next");
        }

        return new LexoRank(findMidpoint(prevVal, nextVal));
    }

    private String findMidpoint(String prev, String next) {
        StringBuilder mid = new StringBuilder();
        int maxLength = Math.max(prev.length(), next.length());

        // Validate up front: rank chars outside the alphabet break position math
        for (char c : (prev + next).toCharArray()) {
            if (RANK_CHARS.indexOf(c) < 0) {
                throw new IllegalArgumentException("Rank contains a character outside the rank alphabet: '" + c + "'");
            }
        }

        for (int i = 0; i < maxLength; i++) {
            char prevChar = i < prev.length() ? prev.charAt(i) : MIN_CHAR;
            char nextChar = i < next.length() ? next.charAt(i) : MAX_CHAR;

            if (prevChar == nextChar) {
                mid.append(prevChar);
                continue;
            }

            int prevIdx = RANK_CHARS.indexOf(prevChar);
            int nextIdx = RANK_CHARS.indexOf(nextChar);
            int midIdx = prevIdx + (nextIdx - prevIdx) / 2;

            if (midIdx == prevIdx) {
                // Adjacent chars — no room at this level; descend with the alphabet's middle char
                mid.append(prevChar);
                mid.append(RANK_CHARS.charAt(RANK_CHARS.length() / 2));
            } else {
                mid.append(RANK_CHARS.charAt(midIdx));
            }
            break;
        }
        return mid.toString();
    }
}