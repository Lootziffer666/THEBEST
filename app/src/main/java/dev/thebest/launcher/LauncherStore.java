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
    private static final String HIDDEN = "hidden";
    private static final String RECENTS = "recents";
    private static final String ACCENT = "accent";

    private final SharedPreferences prefs;

    LauncherStore(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    Set<String> favorites() {
        return new LinkedHashSet<>(prefs.getStringSet(FAVORITES, new LinkedHashSet<>()));
    }

    Set<String> hidden() {
        return new HashSet<>(prefs.getStringSet(HIDDEN, new HashSet<>()));
    }

    List<String> recents() {
        String raw = prefs.getString(RECENTS, "");
        if (raw.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(raw.split("\\n")));
    }

    int accent() {
        return prefs.getInt(ACCENT, 0xff9bf6ff);
    }

    boolean toggleFavorite(String key) {
        Set<String> values = favorites();
        boolean added = values.add(key);
        if (!added) values.remove(key);
        prefs.edit().putStringSet(FAVORITES, values).apply();
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
        prefs.edit().putString(RECENTS, String.join("\n", values)).apply();
    }

    void setAccent(int color) {
        prefs.edit().putInt(ACCENT, color).apply();
    }
}
