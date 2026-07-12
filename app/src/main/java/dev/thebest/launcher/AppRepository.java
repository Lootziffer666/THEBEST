package dev.thebest.launcher;

import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.os.Handler;
import android.os.Process;
import android.os.UserHandle;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class AppRepository {
    interface Listener {
        void onAppsChanged(List<AppEntry> apps);
    }

    private final LauncherApps launcherApps;
    private final Handler mainHandler;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Collator collator = Collator.getInstance(Locale.getDefault());
    private final Object lock = new Object();
    private final LauncherApps.Callback callback = new LauncherApps.Callback() {
        @Override public void onPackageRemoved(String packageName, UserHandle user) { refresh(); }
        @Override public void onPackageAdded(String packageName, UserHandle user) { refresh(); }
        @Override public void onPackageChanged(String packageName, UserHandle user) { refresh(); }
        @Override public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) { refresh(); }
        @Override public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) { refresh(); }
    };

    private Listener listener;
    private List<AppEntry> cachedApps = Collections.emptyList();
    private boolean registered;
    private boolean refreshQueued;

    AppRepository(LauncherApps launcherApps, Handler mainHandler) {
        this.launcherApps = launcherApps;
        this.mainHandler = mainHandler;
    }

    void setListener(Listener listener) {
        this.listener = listener;
        if (!cachedApps.isEmpty()) listener.onAppsChanged(cachedApps);
    }

    void start() {
        if (!registered) {
            launcherApps.registerCallback(callback, mainHandler);
            registered = true;
        }
        refresh();
    }

    void stop() {
        if (registered) {
            launcherApps.unregisterCallback(callback);
            registered = false;
        }
    }

    void shutdown() {
        stop();
        worker.shutdownNow();
    }

    void refresh() {
        synchronized (lock) {
            if (refreshQueued) return;
            refreshQueued = true;
        }
        worker.execute(() -> {
            List<AppEntry> loaded = loadApps();
            mainHandler.post(() -> {
                synchronized (lock) {
                    refreshQueued = false;
                }
                cachedApps = loaded;
                if (listener != null) listener.onAppsChanged(loaded);
            });
        });
    }

    private List<AppEntry> loadApps() {
        List<AppEntry> loaded = new ArrayList<>();
        for (LauncherActivityInfo info : launcherApps.getActivityList(null, Process.myUserHandle())) {
            loaded.add(new AppEntry(info));
        }
        loaded.sort((a, b) -> collator.compare(a.label, b.label));
        return loaded;
    }
}
