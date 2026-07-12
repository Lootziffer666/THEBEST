package dev.thebest.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SearchLensTest {
    @Test public void plainSearchUsesAllLens() {
        SearchLens lens = SearchLens.parse("maps");

        assertTrue(lens.is(SearchLens.ALL));
        assertEquals("maps", lens.query);
    }

    @Test public void favoriteLensKeepsTrailingFilter() {
        SearchLens lens = SearchLens.parse("#fav maps");

        assertTrue(lens.is(SearchLens.FAVORITES));
        assertEquals("maps", lens.query);
    }

    @Test public void recentLensCanBeUnfiltered() {
        SearchLens lens = SearchLens.parse("#recent");

        assertTrue(lens.is(SearchLens.RECENTS));
        assertEquals("", lens.query);
    }

    @Test public void hiddenLensKeepsTrailingFilter() {
        SearchLens lens = SearchLens.parse("#hidden camera");

        assertTrue(lens.is(SearchLens.HIDDEN));
        assertEquals("camera", lens.query);
    }

    @Test public void lensNamesAreNormalized() {
        SearchLens lens = SearchLens.parse("#FÄV über");

        assertTrue(lens.is(SearchLens.FAVORITES));
        assertEquals("über", lens.query);
    }
}
