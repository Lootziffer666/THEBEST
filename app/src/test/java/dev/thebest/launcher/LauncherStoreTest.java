package dev.thebest.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LauncherStoreTest {
    @Test public void favoritesKeepToggleOrder() {
        LauncherStore store = store();

        assertTrue(store.toggleFavorite("camera/.Main"));
        assertTrue(store.toggleFavorite("maps/.Main"));
        assertFalse(store.toggleFavorite("camera/.Main"));
        assertTrue(store.toggleFavorite("camera/.Main"));

        assertEquals(set("maps/.Main", "camera/.Main"), store.favorites());
    }

    @Test public void rememberLaunchKeepsMostRecentFirstAndCapsAtEight() {
        LauncherStore store = store();

        for (int i = 0; i < 10; i++) store.rememberLaunch("app" + i);
        store.rememberLaunch("app5");

        List<String> recents = store.recents();
        assertEquals(8, recents.size());
        assertEquals("app5", recents.get(0));
        assertEquals("app9", recents.get(1));
        assertFalse(recents.contains("app0"));
        assertFalse(recents.contains("app1"));
    }

    @Test public void hiddenAppsToggleIndependentlyFromFavorites() {
        LauncherStore store = store();

        store.toggleFavorite("maps/.Main");
        assertTrue(store.toggleHidden("maps/.Main"));
        assertFalse(store.toggleHidden("maps/.Main"));

        assertTrue(store.hidden().isEmpty());
        assertEquals(set("maps/.Main"), store.favorites());
    }

    @Test public void clearPersonalizationResetsAllUserState() {
        LauncherStore store = store();
        store.toggleFavorite("maps/.Main");
        store.toggleHidden("camera/.Main");
        store.rememberLaunch("clock/.Main");
        store.setAccent(0xffffadad);

        store.clearPersonalization();

        assertTrue(store.favorites().isEmpty());
        assertTrue(store.hidden().isEmpty());
        assertTrue(store.recents().isEmpty());
        assertEquals(0xff9bf6ff, store.accent());
    }

    @Test public void exportTextContainsPersistedSections() {
        LauncherStore store = store();
        store.toggleFavorite("maps/.Main");
        store.toggleHidden("camera/.Main");
        store.rememberLaunch("clock/.Main");

        String backup = store.exportText();

        assertTrue(backup.startsWith("THEBEST backup\n"));
        assertTrue(backup.contains("favorites=maps/.Main"));
        assertTrue(backup.contains("hidden=camera/.Main"));
        assertTrue(backup.contains("recents=clock/.Main"));
        assertTrue(backup.contains("accent=ff9bf6ff"));
    }

    private LauncherStore store() {
        return new LauncherStore(new FakeSharedPreferences());
    }

    private LinkedHashSet<String> set(String... values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Collections.addAll(result, values);
        return result;
    }

    private static final class FakeSharedPreferences implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<>();

        @Override public Map<String, ?> getAll() { return new HashMap<>(values); }
        @Override public String getString(String key, String defValue) { return value(key, String.class, defValue); }
        @Override public Set<String> getStringSet(String key, Set<String> defValues) { return new LinkedHashSet<>(value(key, Set.class, defValues)); }
        @Override public int getInt(String key, int defValue) { return value(key, Integer.class, defValue); }
        @Override public long getLong(String key, long defValue) { return value(key, Long.class, defValue); }
        @Override public float getFloat(String key, float defValue) { return value(key, Float.class, defValue); }
        @Override public boolean getBoolean(String key, boolean defValue) { return value(key, Boolean.class, defValue); }
        @Override public boolean contains(String key) { return values.containsKey(key); }
        @Override public Editor edit() { return new FakeEditor(); }
        @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
        @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        @SuppressWarnings("unchecked")
        private <T> T value(String key, Class<?> type, T fallback) {
            Object value = values.get(key);
            if (type.isInstance(value)) return (T) value;
            return fallback;
        }

        private final class FakeEditor implements Editor {
            private final Map<String, Object> pending = new HashMap<>();
            private final Set<String> removals = new HashSet<>();
            private boolean clear;

            @Override public Editor putString(String key, String value) { pending.put(key, value); return this; }
            @Override public Editor putStringSet(String key, Set<String> value) { pending.put(key, new LinkedHashSet<>(value)); return this; }
            @Override public Editor putInt(String key, int value) { pending.put(key, value); return this; }
            @Override public Editor putLong(String key, long value) { pending.put(key, value); return this; }
            @Override public Editor putFloat(String key, float value) { pending.put(key, value); return this; }
            @Override public Editor putBoolean(String key, boolean value) { pending.put(key, value); return this; }
            @Override public Editor remove(String key) { removals.add(key); return this; }
            @Override public Editor clear() { clear = true; return this; }
            @Override public boolean commit() { apply(); return true; }
            @Override public void apply() {
                if (clear) values.clear();
                for (String key : removals) values.remove(key);
                values.putAll(pending);
            }
        }
    }
}
