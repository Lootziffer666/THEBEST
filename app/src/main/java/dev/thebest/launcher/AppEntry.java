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
        this(info.getLabel().toString(), info.getComponentName().flattenToString(), info.getComponentName().getPackageName(), info);
    }

    AppEntry(String label, String packageName) {
        this(label, packageName + "/.MainActivity", packageName, null);
    }

    private AppEntry(String label, String key, String packageName, LauncherActivityInfo info) {
        this.info = info;
        this.label = label;
        this.key = key;
        this.packageName = packageName;
        this.searchable = normalize(label + " " + packageName);
    }

    boolean matches(String query) {
        return query.isEmpty() || searchable.contains(normalize(query));
    }

    static String normalize(String value) {
        if (value == null) return "";
        String transliterated = value.replace("ß", "ss").replace("ẞ", "SS");
        String decomposed = Normalizer.normalize(transliterated, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase(Locale.getDefault()).trim();
    }
}
