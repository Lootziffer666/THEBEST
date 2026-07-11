package dev.thebest.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private final List<AppEntry> apps = new ArrayList<>();
    private final List<AppEntry> visible = new ArrayList<>();
    private final Map<String, AppEntry> byKey = new HashMap<>();
    private final Collator collator = Collator.getInstance(Locale.getDefault());
    private final SearchRanker ranker = new SearchRanker();

    private LauncherStore store;
    private LinearLayout root;
    private LinearLayout smartRow;
    private LinearLayout favoritesRow;
    private LinearLayout sectionList;
    private EditText search;
    private TextView status;
    private float touchDownY;
    private int accent;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new LauncherStore(getSharedPreferences("thebest", MODE_PRIVATE));
        accent = store.accent();
        buildUi();
        reloadApps();
        applyQuery("");
    }

    @Override protected void onResume() {
        super.onResume();
        reloadApps();
        applyQuery(search == null ? "" : search.getText().toString());
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(10));
        root.setBackground(makeBackdrop());
        root.setOnTouchListener((view, event) -> handleGesture(event));
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        header.addView(titleBlock, new LinearLayout.LayoutParams(0, -2, 1));

        TextView title = text("THEBEST", 34, Color.WHITE, true);
        titleBlock.addView(title);
        TextView line = text("calm launcher · local brain · fast hands", 13, 0xffaeb7c2, false);
        titleBlock.addView(line);

        TextView menu = pill("⚙", 0xff151922, Color.WHITE);
        menu.setTextSize(24);
        menu.setOnClickListener(v -> showLauncherMenu(v));
        header.addView(menu, new LinearLayout.LayoutParams(dp(56), dp(48)));

        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Type an intention — e.g. cam, maps, pay, :all…");
        search.setHintTextColor(0xff7d8793);
        search.setTextColor(Color.WHITE);
        search.setTextSize(18);
        search.setPadding(dp(18), 0, dp(18), 0);
        search.setMinHeight(dp(56));
        search.setContentDescription("Search apps and launcher commands");
        search.setBackground(round(0xff151922, 28, 0xff2a3240));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(58));
        searchParams.setMargins(0, dp(18), 0, dp(10));
        root.addView(search, searchParams);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { applyQuery(s.toString()); }
            public void afterTextChanged(Editable e) {}
        });

        smartRow = rowScroll(dp(76));
        favoritesRow = rowScroll(dp(98));

        status = text("", 12, 0xff7d8793, false);
        status.setGravity(Gravity.END);
        root.addView(status);

        ScrollView scroll = new ScrollView(this);
        sectionList = new LinearLayout(this);
        sectionList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(sectionList);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private LinearLayout rowScroll(int height) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.addView(row);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, height));
        return row;
    }

    private void reloadApps() {
        apps.clear();
        byKey.clear();
        LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        for (LauncherActivityInfo info : launcherApps.getActivityList(null, Process.myUserHandle())) {
            AppEntry app = new AppEntry(info);
            apps.add(app);
            byKey.put(app.key, app);
        }
        apps.sort((a, b) -> collator.compare(a.label, b.label));
    }

    private void applyQuery(String raw) {
        String query = raw.trim();
        if (query.startsWith(":")) {
            renderCommandPalette(query);
            return;
        }
        Set<String> hidden = store.hidden();
        Set<String> favorites = store.favorites();
        List<String> recents = store.recents();
        visible.clear();
        visible.addAll(ranker.rank(apps, query, favorites, recents, hidden));
        renderSmartRow(query);
        renderFavorites(hidden);
        renderFocusResults(query, visible);
        status.setText(statusLine(query, visible.size(), hidden.size()));
    }

    private void renderSmartRow(String query) {
        smartRow.removeAllViews();
        if (!query.isEmpty()) {
            smartRow.addView(actionCard("↵", "launch first", v -> { if (!visible.isEmpty()) launch(visible.get(0)); }));
            smartRow.addView(actionCard("⌫", "clear", v -> search.setText("")));
            return;
        }
        for (String key : store.recents()) {
            AppEntry app = byKey.get(key);
            if (app != null) smartRow.addView(compactApp(app));
        }
        smartRow.addView(actionCard(":", "commands", v -> focusCommand()));
        smartRow.addView(actionCard("◎", "settings", v -> openSystemSettings()));
    }

    private void renderFavorites(Set<String> hidden) {
        favoritesRow.removeAllViews();
        for (String key : store.favorites()) {
            AppEntry app = byKey.get(key);
            if (app != null && !hidden.contains(key)) favoritesRow.addView(appTile(app, true));
        }
        if (favoritesRow.getChildCount() == 0) {
            favoritesRow.addView(emptyCard("Long-press any app to pin it here."));
        }
    }

    private void renderFocusResults(String query, List<AppEntry> list) {
        sectionList.removeAllViews();
        if (query.isEmpty()) {
            renderHomePrompt(list);
            return;
        }
        if (list.isEmpty()) {
            sectionList.addView(emptyCard("No app found. Swipe down to retry or type :all."));
            return;
        }
        int limit = Math.min(12, list.size());
        for (int i = 0; i < limit; i++) sectionList.addView(resultCard(list.get(i), i));
        if (list.size() > limit) sectionList.addView(emptyCard((list.size() - limit) + " more matches — keep typing to narrow."));
    }

    private List<AppEntry> libraryApps() {
        Set<String> hidden = store.hidden();
        List<AppEntry> result = new ArrayList<>();
        for (AppEntry app : apps) if (!hidden.contains(app.key)) result.add(app);
        return result;
    }

    private void renderLibrary(List<AppEntry> list) {
        sectionList.removeAllViews();
        if (list.isEmpty()) {
            sectionList.addView(emptyCard("No app found. Try another query or :hidden."));
            return;
        }
        Map<String, List<AppEntry>> sections = new LinkedHashMap<>();
        for (AppEntry app : list) {
            String first = app.label.isEmpty() ? "#" : app.label.substring(0, 1).toUpperCase(Locale.getDefault());
            if (!first.matches("[A-ZÄÖÜ0-9]")) first = "#";
            if (!sections.containsKey(first)) sections.put(first, new ArrayList<>());
            sections.get(first).add(app);
        }
        for (Map.Entry<String, List<AppEntry>> section : sections.entrySet()) {
            TextView header = text(section.getKey(), 13, accent, true);
            header.setPadding(dp(4), dp(14), 0, dp(6));
            sectionList.addView(header);
            GridLayout grid = new GridLayout(this);
            grid.setColumnCount(4);
            for (AppEntry app : section.getValue()) grid.addView(appTile(app, false));
            sectionList.addView(grid);
        }
    }

    private void renderCommandPalette(String query) {
        smartRow.removeAllViews();
        favoritesRow.removeAllViews();
        sectionList.removeAllViews();
        status.setText("command palette · no cloud needed");
        addCommand(":theme", "cycle accent color", v -> cycleTheme());
        addCommand(":hidden", "review hidden apps", v -> showHiddenApps());
        addCommand(":settings", "open Android settings", v -> openSystemSettings());
        addCommand(":apps", "open Android app settings", v -> startActivity(new Intent(Settings.ACTION_APPLICATION_SETTINGS)));
        addCommand(":all", "open the full alphabetised library", v -> renderLibrary(libraryApps()));
        addCommand(":about", "explain THEBEST", v -> showAbout());
    }

    private void addCommand(String command, String detail, View.OnClickListener listener) {
        TextView card = pill(command + "\n" + detail, 0xff151922, Color.WHITE);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setTextSize(16);
        card.setMinHeight(dp(48));
        card.setContentDescription(command + " command: " + detail);
        card.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(70));
        params.setMargins(0, dp(5), 0, dp(5));
        sectionList.addView(card, params);
    }

    private void renderHomePrompt(List<AppEntry> ranked) {
        TextView prompt = emptyCard("Swipe down: search · type : for commands · long-press apps for control");
        sectionList.addView(prompt, new LinearLayout.LayoutParams(-1, dp(74)));
        TextView header = text("Predicted essentials", 13, accent, true);
        header.setPadding(dp(4), dp(18), 0, dp(8));
        sectionList.addView(header);
        int count = Math.min(8, ranked.size());
        if (count == 0) {
            sectionList.addView(emptyCard("Launch or pin apps and this space becomes personal."));
            return;
        }
        for (int i = 0; i < count; i++) sectionList.addView(resultCard(ranked.get(i), i));
    }

    private View resultCard(AppEntry app, int index) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), 0, dp(14), 0);
        card.setMinimumHeight(dp(68));
        card.setBackground(round(index == 0 ? 0xff1d2a36 : 0xff11151d, 24, index == 0 ? accent : 0xff202938));
        card.setContentDescription("Launch " + app.label);

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(app.info.getIcon(0));
        card.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(14), 0, 0, 0);
        TextView label = text(app.label, index == 0 ? 18 : 16, Color.WHITE, index == 0);
        TextView packageName = text(app.packageName, 12, 0xff7d8793, false);
        copy.addView(label);
        copy.addView(packageName);
        card.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));

        TextView action = text(index == 0 ? "↵" : "open", index == 0 ? 24 : 12, accent, true);
        card.addView(action);
        card.setOnClickListener(v -> launch(app));
        card.setOnLongClickListener(v -> { showAppMenu(v, app); return true; });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(76));
        params.setMargins(0, dp(5), 0, dp(5));
        card.setLayoutParams(params);
        return card;
    }

    private String statusLine(String query, int resultCount, int hiddenCount) {
        if (query.isEmpty()) return "quiet home · " + hiddenCount + " hidden · all local";
        return resultCount + " ranked matches · " + hiddenCount + " hidden · all local";
    }

    private View appTile(AppEntry app, boolean favorite) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(6), dp(8), dp(6), dp(8));
        box.setMinimumHeight(dp(56));
        box.setContentDescription("Launch " + app.label);
        box.setBackground(round(favorite ? 0xff18212d : 0x00000000, 24, favorite ? accent : 0x00000000));
        int width = favorite ? dp(88) : getResources().getDisplayMetrics().widthPixels / 4 - dp(10);
        ViewGroup.LayoutParams params;
        if (favorite) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, dp(94));
            p.setMargins(0, 0, dp(8), 0);
            params = p;
        } else {
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = width;
            p.height = dp(94);
            p.setMargins(dp(2), dp(4), dp(2), dp(4));
            params = p;
        }
        box.setLayoutParams(params);

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(app.info.getIcon(0));
        box.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView label = text(app.label, 12, Color.WHITE, false);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        box.addView(label, new LinearLayout.LayoutParams(-1, -2));
        box.setOnClickListener(v -> launch(app));
        box.setOnLongClickListener(v -> { showAppMenu(v, app); return true; });
        return box;
    }

    private View compactApp(AppEntry app) {
        TextView card = pill(app.label, 0xff151922, Color.WHITE);
        card.setContentDescription("Recent app: " + app.label);
        card.setCompoundDrawablesWithIntrinsicBounds(app.info.getIcon(0), null, null, null);
        card.setCompoundDrawablePadding(dp(8));
        card.setOnClickListener(v -> launch(app));
        card.setOnLongClickListener(v -> { showAppMenu(v, app); return true; });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(154), dp(58));
        params.setMargins(0, dp(8), dp(8), dp(8));
        card.setLayoutParams(params);
        return card;
    }

    private View actionCard(String symbol, String label, View.OnClickListener listener) {
        TextView card = pill(symbol + "\n" + label, 0xff151922, Color.WHITE);
        card.setTextSize(14);
        card.setGravity(Gravity.CENTER);
        card.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(112), dp(58));
        params.setMargins(0, dp(8), dp(8), dp(8));
        card.setLayoutParams(params);
        return card;
    }

    private TextView emptyCard(String message) {
        TextView empty = pill(message, 0xff11151d, 0xffaeb7c2);
        empty.setGravity(Gravity.CENTER_VERTICAL);
        empty.setPadding(dp(18), 0, dp(18), 0);
        return empty;
    }

    private void showAppMenu(View anchor, AppEntry app) {
        anchor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        PopupMenu menu = new PopupMenu(this, anchor);
        boolean favorite = store.favorites().contains(app.key);
        boolean hidden = store.hidden().contains(app.key);
        menu.getMenu().add(favorite ? "Unpin favourite" : "Pin favourite");
        menu.getMenu().add(hidden ? "Unhide app" : "Hide app");
        menu.getMenu().add("App info");
        menu.getMenu().add("Uninstall");
        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.contains("favourite")) {
                store.toggleFavorite(app.key);
                toast(favorite ? "Unpinned " + app.label : "Pinned " + app.label);
            } else if (title.contains("Hide") || title.contains("Unhide")) {
                store.toggleHidden(app.key);
                toast(hidden ? "Unhidden " + app.label : "Hidden " + app.label);
            } else if (title.equals("App info")) {
                openAppInfo(app);
                return true;
            } else {
                startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.packageName)));
                return true;
            }
            applyQuery(search.getText().toString());
            return true;
        });
        menu.show();
    }

    private void showLauncherMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Command palette");
        menu.getMenu().add("Cycle accent");
        menu.getMenu().add("Hidden apps");
        menu.getMenu().add("About THEBEST");
        menu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.startsWith("Command")) focusCommand();
            else if (title.startsWith("Cycle")) cycleTheme();
            else if (title.startsWith("Hidden")) showHiddenApps();
            else showAbout();
            return true;
        });
        menu.show();
    }

    private void showHiddenApps() {
        Set<String> hidden = store.hidden();
        if (hidden.isEmpty()) {
            toast("No hidden apps");
            return;
        }
        String[] labels = new String[hidden.size()];
        String[] keys = hidden.toArray(new String[0]);
        for (int i = 0; i < keys.length; i++) {
            AppEntry app = byKey.get(keys[i]);
            labels[i] = app == null ? keys[i] : app.label;
        }
        new AlertDialog.Builder(this)
                .setTitle("Hidden apps")
                .setItems(labels, (dialog, which) -> { store.toggleHidden(keys[which]); applyQuery(""); })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("THEBEST")
                .setMessage("A launcher experiment: search first, favourites near your thumb, commands when useful, and no Internet permission.")
                .setPositiveButton("Nice", null)
                .show();
    }

    private void launch(AppEntry app) {
        try {
            ComponentName component = ComponentName.unflattenFromString(app.key);
            Intent intent = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(component)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            store.rememberLaunch(app.key);
            startActivity(intent);
            search.setText("");
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(), 0);
        } catch (Exception ex) {
            toast("Cannot launch " + app.label);
        }
    }

    private void openAppInfo(AppEntry app) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + app.packageName));
        startActivity(intent);
    }

    private void openSystemSettings() {
        startActivity(new Intent(Settings.ACTION_SETTINGS));
    }

    private void cycleTheme() {
        int[] colors = {0xff9bf6ff, 0xffffadad, 0xffcaffbf, 0xffffd6a5, 0xffbdb2ff};
        int next = colors[0];
        for (int i = 0; i < colors.length; i++) if (colors[i] == accent) next = colors[(i + 1) % colors.length];
        accent = next;
        store.setAccent(accent);
        root.setBackground(makeBackdrop());
        applyQuery(search.getText().toString());
    }

    private void focusCommand() {
        search.setText(":");
        search.setSelection(search.length());
        search.requestFocus();
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(search, InputMethodManager.SHOW_IMPLICIT);
    }

    private boolean handleGesture(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            touchDownY = event.getY();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float delta = event.getY() - touchDownY;
            if (delta > dp(70)) {
                search.requestFocus();
                ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(search, InputMethodManager.SHOW_IMPLICIT);
                return true;
            }
            if (delta < -dp(70)) {
                search.setText(":");
                search.setSelection(search.length());
                return true;
            }
        }
        return false;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(sp);
        if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private TextView pill(String value, int color, int textColor) {
        TextView view = text(value, 14, textColor, false);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), 0, dp(12), 0);
        view.setBackground(round(color, 24, 0xff2a3240));
        return view;
    }

    private GradientDrawable round(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if (stroke != 0) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private GradientDrawable makeBackdrop() {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{darken(accent), 0xff06070a, 0xff06070a});
    }

    private int darken(int color) {
        return Color.rgb((int) (Color.red(color) * 0.12), (int) (Color.green(color) * 0.12), (int) (Color.blue(color) * 0.16));
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
