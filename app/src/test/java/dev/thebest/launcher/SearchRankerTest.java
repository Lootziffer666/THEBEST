package dev.thebest.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SearchRankerTest {
    private final SearchRanker ranker = new SearchRanker();

    @Test public void prefixMatchesRankAboveContains() {
        List<AppEntry> result = rank("map", apps("Google Maps", "Citymapper"));

        assertEquals("Google Maps", result.get(0).label);
        assertEquals("Citymapper", result.get(1).label);
    }

    @Test public void wordBoundaryMatchesRankAboveContains() {
        List<AppEntry> result = rank("map", apps("Transit Mapper", "Citymapper"));

        assertEquals("Transit Mapper", result.get(0).label);
    }

    @Test public void initialsMatchMultiWordApps() {
        List<AppEntry> result = rank("gm", apps("Google Maps", "Gallery"));

        assertEquals(1, result.size());
        assertEquals("Google Maps", result.get(0).label);
    }

    @Test public void subsequenceStillMatchesSingleTokenAbbreviations() {
        List<AppEntry> result = rank("cmr", apps("Camera", "Maps"));

        assertEquals(1, result.size());
        assertEquals("Camera", result.get(0).label);
    }

    @Test public void oneEditTypoMatchesToken() {
        List<AppEntry> result = rank("gogle", apps("Google Maps", "Gallery"));

        assertEquals(1, result.size());
        assertEquals("Google Maps", result.get(0).label);
    }

    @Test public void umlautsAccentsAndSharpSAreNormalized() {
        List<AppEntry> result = rank("strasse", apps("Straße", "Über", "Cafe"));

        assertEquals(1, result.size());
        assertEquals("Straße", result.get(0).label);
    }

    @Test public void hiddenAppsAreExcluded() {
        AppEntry visible = app("Camera");
        AppEntry hidden = app("Calendar");
        Set<String> hiddenKeys = new LinkedHashSet<>(Collections.singletonList(hidden.key));

        List<AppEntry> result = ranker.rank(Arrays.asList(hidden, visible), "ca", Collections.emptySet(), Collections.emptyList(), hiddenKeys);

        assertEquals(1, result.size());
        assertEquals("Camera", result.get(0).label);
    }

    @Test public void emptyQueryPromotesFavoritesAndRecents() {
        AppEntry plain = app("Browser");
        AppEntry recent = app("Clock");
        AppEntry favorite = app("Notes");

        List<AppEntry> result = ranker.rank(
                Arrays.asList(plain, recent, favorite),
                "",
                new LinkedHashSet<>(Collections.singletonList(favorite.key)),
                Collections.singletonList(recent.key),
                Collections.emptySet());

        assertEquals("Notes", result.get(0).label);
        assertEquals("Clock", result.get(1).label);
        assertEquals("Browser", result.get(2).label);
    }

    @Test public void unknownQueryReturnsNoResults() {
        assertTrue(rank("zzz", apps("Camera", "Maps")).isEmpty());
    }

    @Test public void explanationsExposeScoreBreakdown() {
        AppEntry maps = app("Google Maps");
        List<SearchRanker.RankExplanation> explanations = ranker.explain(
                Collections.singletonList(maps),
                "gm",
                new LinkedHashSet<>(Collections.singletonList(maps.key)),
                Collections.singletonList(maps.key),
                Collections.emptySet());

        assertEquals(1, explanations.size());
        assertEquals(maps, explanations.get(0).app);
        assertTrue(explanations.get(0).score > 0);
        assertTrue(explanations.get(0).summary().contains("initials +98"));
        assertTrue(explanations.get(0).summary().contains("favorite +22"));
        assertTrue(explanations.get(0).summary().contains("recent +14"));
    }

    private List<AppEntry> rank(String query, List<AppEntry> apps) {
        return ranker.rank(apps, query, Collections.emptySet(), Collections.emptyList(), Collections.emptySet());
    }

    private List<AppEntry> apps(String... labels) {
        AppEntry[] entries = new AppEntry[labels.length];
        for (int i = 0; i < labels.length; i++) entries[i] = app(labels[i]);
        return Arrays.asList(entries);
    }

    private AppEntry app(String label) {
        return new AppEntry(label, "dev.test." + AppEntry.normalize(label).replace(' ', '.'));
    }
}
