package dev.thebest.launcher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class SearchRanker {
    List<AppEntry> rank(List<AppEntry> apps, String rawQuery, Set<String> favorites, List<String> recents, Set<String> hidden) {
        String query = AppEntry.normalize(rawQuery);
        Set<String> recentSet = new HashSet<>(recents);
        List<ScoredApp> scored = new ArrayList<>();
        for (AppEntry app : apps) {
            if (hidden.contains(app.key)) continue;
            int score = score(app, query, favorites.contains(app.key), recentSet.contains(app.key));
            if (score > 0) scored.add(new ScoredApp(app, score));
        }
        scored.sort(Comparator.<ScoredApp>comparingInt(item -> item.score).reversed().thenComparing(item -> item.app.label));
        List<AppEntry> result = new ArrayList<>();
        for (ScoredApp item : scored) result.add(item.app);
        return result;
    }

    private int score(AppEntry app, String query, boolean favorite, boolean recent) {
        int score = 0;
        if (query.isEmpty()) {
            score = 10;
            if (favorite) score += 60;
            if (recent) score += 45;
            return score;
        }
        if (app.searchable.startsWith(query)) score += 120;
        else if (app.searchable.contains(" " + query)) score += 105;
        else if (app.searchable.contains(query)) score += 80;
        else if (subsequence(app.searchable, query)) score += 52;
        else return 0;
        if (favorite) score += 22;
        if (recent) score += 14;
        score -= Math.min(24, app.label.length() / 2);
        return score;
    }

    private boolean subsequence(String haystack, String needle) {
        int at = 0;
        for (int i = 0; i < haystack.length() && at < needle.length(); i++) {
            if (haystack.charAt(i) == needle.charAt(at)) at++;
        }
        return at == needle.length();
    }

    private static final class ScoredApp {
        final AppEntry app;
        final int score;

        ScoredApp(AppEntry app, int score) {
            this.app = app;
            this.score = score;
        }
    }
}
