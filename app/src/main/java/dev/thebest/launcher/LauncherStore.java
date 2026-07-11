package dev.thebest.launcher;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class LauncherStore {
    private static final String FAVORITES = "favorites";
    private static final String FAVORITES_ORDER = "favorites_order";
    private static final String HIDDEN = "hidden";
    private static final String RECENTS = "recents";
    private static final String ACCENT = "accent";

    private final SharedPreferences prefs;

    LauncherStore(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    Set<String> favorites() {
        LinkedHashSet<String> ordered = orderedSet(FAVORITES_ORDER);
        if (!ordered.isEmpty()) return ordered;
        return new LinkedHashSet<>(prefs.getStringSet(FAVORITES, new LinkedHashSet<>()));
    }

    Set<String> hidden() {
        return new HashSet<>(prefs.getStringSet(HIDDEN, new HashSet<>()));
    }

    List<String> recents() {
        return new ArrayList<>(orderedSet(RECENTS));
    }

    int accent() {
        return prefs.getInt(ACCENT, 0xff9bf6ff);
    }

    boolean toggleFavorite(String key) {
        LinkedHashSet<String> values = new LinkedHashSet<>(favorites());
        boolean added = values.add(key);
        if (!added) values.remove(key);
        prefs.edit()
                .putStringSet(FAVORITES, values)
                .putString(FAVORITES_ORDER, join(values))
                .apply();
        return added;
    }

    boolean toggleHidden(String key) {
        Set<String> values = hidden();
        boolean added = values.add(key);
        if (!added) values.remove(key);
        prefs.edit().putStringSet(HIDDEN, values).apply();
        return added;
    }

    void rememberLaunch(String key) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add(key);
        for (String old : recents()) {
            if (values.size() >= 8) break;
            values.add(old);
        }
        prefs.edit().putString(RECENTS, join(values)).apply();
    }

    void setAccent(int color) {
        prefs.edit().putInt(ACCENT, color).apply();
    }

    void clearPersonalization() {
        prefs.edit()
                .remove(FAVORITES)
                .remove(FAVORITES_ORDER)
                .remove(HIDDEN)
                .remove(RECENTS)
                .remove(ACCENT)
                .apply();
    }

    String exportText() {
        return "THEBEST backup\n"
                + "favorites=" + join(favorites()) + "\n"
                + "hidden=" + join(hidden()) + "\n"
                + "recents=" + join(new LinkedHashSet<>(recents())) + "\n"
                + "accent=" + Integer.toHexString(accent());
    }

    private LinkedHashSet<String> orderedSet(String key) {
        String raw = prefs.getString(key, "");
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (raw.isEmpty()) return values;
        values.addAll(Arrays.asList(raw.split("\\n")));
        values.remove("");
        return values;
    }

    private String join(Set<String> values) {
        return String.join("\n", values);
    }
}
