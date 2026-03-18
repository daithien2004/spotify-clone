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
    public void should_ReturnExtendedChar_when_NoGap() {
        Assertions.assertEquals(new LexoRank("am"), lexoRankService.calculateMid(new LexoRank("a"), new LexoRank("b")));
    }

    @Test
    public void should_ReturnBeforeNext_when_PrevIsNull() {
        Assertions.assertEquals(new LexoRank("m"), lexoRankService.calculateMid(null, new LexoRank("z")));
    }
}
