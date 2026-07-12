package dev.thebest.launcher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class SearchRanker {
    List<AppEntry> rank(List<AppEntry> apps, String rawQuery, Set<String> favorites, List<String> recents, Set<String> hidden) {
        List<RankExplanation> explanations = explain(apps, rawQuery, favorites, recents, hidden);
        List<AppEntry> result = new ArrayList<>();
        for (RankExplanation item : explanations) result.add(item.app);
        return result;
    }

    List<RankExplanation> explain(List<AppEntry> apps, String rawQuery, Set<String> favorites, List<String> recents, Set<String> hidden) {
        String query = AppEntry.normalize(rawQuery);
        Set<String> recentSet = new HashSet<>(recents);
        List<RankExplanation> scored = new ArrayList<>();
        for (AppEntry app : apps) {
            if (hidden.contains(app.key)) continue;
            RankExplanation explanation = score(app, query, favorites.contains(app.key), recentSet.contains(app.key));
            if (explanation.score > 0) scored.add(explanation);
        }
        scored.sort(Comparator.<RankExplanation>comparingInt(item -> item.score).reversed().thenComparing(item -> item.app.label));
        return scored;
    }

    private RankExplanation score(AppEntry app, String query, boolean favorite, boolean recent) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        if (query.isEmpty()) {
            score = 10;
            reasons.add("baseline +10");
            if (favorite) {
                score += 60;
                reasons.add("favorite +60");
            }
            if (recent) {
                score += 45;
                reasons.add("recent +45");
            }
            return new RankExplanation(app, score, reasons);
        }
        if (app.searchable.startsWith(query)) {
            score += 120;
            reasons.add("prefix +120");
        } else if (app.searchable.contains(" " + query)) {
            score += 105;
            reasons.add("word +105");
        } else if (initials(app.searchable).startsWith(query)) {
            score += 98;
            reasons.add("initials +98");
        } else if (hasNearToken(app.searchable, query)) {
            score += 90;
            reasons.add("typo +90");
        } else if (app.searchable.contains(query)) {
            score += 80;
            reasons.add("contains +80");
        } else if (subsequence(app.searchable, query)) {
            score += 52;
            reasons.add("subsequence +52");
        } else {
            return new RankExplanation(app, 0, reasons);
        }
        if (favorite) {
            score += 22;
            reasons.add("favorite +22");
        }
        if (recent) {
            score += 14;
            reasons.add("recent +14");
        }
        int lengthPenalty = Math.min(24, app.label.length() / 2);
        score -= lengthPenalty;
        if (lengthPenalty > 0) reasons.add("length -" + lengthPenalty);
        return new RankExplanation(app, score, reasons);
    }

    private String initials(String value) {
        StringBuilder result = new StringBuilder();
        boolean nextInitial = true;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isLetterOrDigit(current)) {
                if (nextInitial) result.append(current);
                nextInitial = false;
            } else {
                nextInitial = true;
            }
        }
        return result.toString();
    }

    private boolean hasNearToken(String searchable, String query) {
        if (query.length() < 3) return false;
        String[] tokens = searchable.split("[^a-z0-9]+");
        for (String token : tokens) {
            if (editDistanceAtMostOne(token, query)) return true;
        }
        return false;
    }

    private boolean editDistanceAtMostOne(String left, String right) {
        int leftLength = left.length();
        int rightLength = right.length();
        if (Math.abs(leftLength - rightLength) > 1) return false;
        int i = 0;
        int j = 0;
        int edits = 0;
        while (i < leftLength && j < rightLength) {
            if (left.charAt(i) == right.charAt(j)) {
                i++;
                j++;
            } else {
                edits++;
                if (edits > 1) return false;
                if (leftLength > rightLength) i++;
                else if (rightLength > leftLength) j++;
                else {
                    i++;
                    j++;
                }
            }
        }
        if (i < leftLength || j < rightLength) edits++;
        return edits <= 1;
    }

    private boolean subsequence(String haystack, String needle) {
        int at = 0;
        for (int i = 0; i < haystack.length() && at < needle.length(); i++) {
            if (haystack.charAt(i) == needle.charAt(at)) at++;
        }
        return at == needle.length();
    }

    static final class RankExplanation {
        final AppEntry app;
        final int score;
        final List<String> reasons;

        RankExplanation(AppEntry app, int score, List<String> reasons) {
            this.app = app;
            this.score = score;
            this.reasons = reasons;
        }

        String summary() {
            return String.join(" · ", reasons);
        }
    }
}
