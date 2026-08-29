package com.spotify.playlist.domain.service;

import com.spotify.playlist.domain.valueobject.LexoRank;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LexoRankServiceTest {

    private final LexoRankService lexoRankService = new LexoRankService();

    @Test
    public void should_ReturnMidObject_when_BothNull() {
        Assertions.assertEquals(new LexoRank("m"), lexoRankService.calculateMid(null, null));
    }

    @Test
    public void should_ReturnMidChar_when_GapExists() {
        Assertions.assertEquals(new LexoRank("b"), lexoRankService.calculateMid(new LexoRank("a"), new LexoRank("c")));
        Assertions.assertEquals(new LexoRank("i"), lexoRankService.calculateMid(new LexoRank("h"), new LexoRank("j")));
    }

    @Test
    public void should_UseFullCharset_when_GapSpansWholeRange() {
        Assertions.assertEquals(new LexoRank("V"), lexoRankService.calculateMid(new LexoRank("0"), new LexoRank("z")));
    }

    @Test
    public void should_ReturnExtendedChar_when_NoGapAtLevel() {
        Assertions.assertEquals(new LexoRank("aV"), lexoRankService.calculateMid(new LexoRank("a"), new LexoRank("b")));
    }

    @Test
    public void should_ReturnMidRanks_between_NumericRebalancedRanks() {
        // After a rebalance ranks look like "00001000"/"00002000" — midpoint must stay inside a valid charset
        Assertions.assertEquals(new LexoRank("00001V"),
                lexoRankService.calculateMid(new LexoRank("00001000"), new LexoRank("00002000")));
        Assertions.assertEquals(new LexoRank("00002V"),
                lexoRankService.calculateMid(new LexoRank("00002000"), new LexoRank("00003000")));
    }

    @Test
    public void should_ReturnBeforeNext_when_PrevIsNull() {
        Assertions.assertEquals(new LexoRank("V"), lexoRankService.calculateMid(null, new LexoRank("z")));
    }

    @Test
    public void should_ComputeMidpoint_when_NextIsNull() {
        Assertions.assertEquals(new LexoRank("w"), lexoRankService.calculateMid(new LexoRank("t"), null));
    }

    @Test
    public void should_Throw_when_PrevIsNotLessThanNext() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> lexoRankService.calculateMid(new LexoRank("z"), new LexoRank("a")));
    }
}