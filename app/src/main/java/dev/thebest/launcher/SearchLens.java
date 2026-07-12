package dev.thebest.launcher;

final class SearchLens {
    static final String ALL = "all";
    static final String FAVORITES = "favorites";
    static final String RECENTS = "recents";
    static final String HIDDEN = "hidden";

    final String name;
    final String query;

    private SearchLens(String name, String query) {
        this.name = name;
        this.query = query;
    }

    static SearchLens parse(String rawQuery) {
        String trimmed = rawQuery.trim();
        String normalized = AppEntry.normalize(trimmed);
        if (normalized.equals("#fav") || normalized.startsWith("#fav ")) {
            return new SearchLens(FAVORITES, trimmed.substring(Math.min(trimmed.length(), 4)).trim());
        }
        if (normalized.equals("#recent") || normalized.startsWith("#recent ")) {
            return new SearchLens(RECENTS, trimmed.substring(Math.min(trimmed.length(), 7)).trim());
        }
        if (normalized.equals("#hidden") || normalized.startsWith("#hidden ")) {
            return new SearchLens(HIDDEN, trimmed.substring(Math.min(trimmed.length(), 7)).trim());
        }
        return new SearchLens(ALL, trimmed);
    }

    boolean is(String expected) {
        return name.equals(expected);
    }

    boolean hasFilter() {
        return !query.isEmpty();
    }
}
