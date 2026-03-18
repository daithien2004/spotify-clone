package com.spotify.playlist.domain.service;

import com.spotify.playlist.domain.valueobject.LexoRank;

public class LexoRankService {
    private static final int ALPHABET_START = 'a';
    private static final int ALPHABET_END = 'z';

    public LexoRank getInitialRank() {
        return new LexoRank("m");
    }

    public LexoRank calculateMid(LexoRank prev, LexoRank next) {
        if (prev == null && next == null) {
            return getInitialRank();
        }
        
        String prevVal = (prev == null) ? String.valueOf((char) ALPHABET_START) : prev.value();
        String nextVal = (next == null) ? String.valueOf((char) ALPHABET_END) : next.value();

        if (prevVal.compareTo(nextVal) >= 0) {
            throw new IllegalArgumentException("prev must be strictly less than next");
        }

        return new LexoRank(findMidpoint(prevVal, nextVal));
    }

    private String findMidpoint(String prev, String next) {
        StringBuilder mid = new StringBuilder();
        int maxLength = Math.max(prev.length(), next.length());
        
        for (int i = 0; i < maxLength; i++) {
            int prevChar = i < prev.length() ? prev.charAt(i) : ALPHABET_START;
            int nextChar = i < next.length() ? next.charAt(i) : ALPHABET_END;
            
            if (prevChar == nextChar) {
                mid.append((char) prevChar);
            } else {
                int midpointChar = prevChar + (nextChar - prevChar) / 2;
                
                if (midpointChar == prevChar) {
                    mid.append((char) prevChar);
                    mid.append((char) (ALPHABET_START + (ALPHABET_END - ALPHABET_START) / 2));
                } else {
                    mid.append((char) midpointChar);
                }
                break;
            }
        }
        return mid.toString();
    }
}
