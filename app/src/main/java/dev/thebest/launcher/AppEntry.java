package dev.thebest.launcher;

import android.content.pm.LauncherActivityInfo;

import java.text.Normalizer;
import java.util.Locale;

final class AppEntry {
    final String label;
    final String key;
    final String packageName;
    final LauncherActivityInfo info;
    final String searchable;

    AppEntry(LauncherActivityInfo info) {
        this.info = info;
        this.label = info.getLabel().toString();
        this.key = info.getComponentName().flattenToString();
        this.packageName = info.getComponentName().getPackageName();
        this.searchable = normalize(label + " " + packageName);
    }

    boolean matches(String query) {
        return query.isEmpty() || searchable.contains(normalize(query));
    }

    static String normalize(String value) {
        if (value == null) return "";
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase(Locale.getDefault()).trim();
    }
}
