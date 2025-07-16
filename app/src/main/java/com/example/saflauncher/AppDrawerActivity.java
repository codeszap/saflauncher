package com.example.saflauncher;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.*;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.*;

public class AppDrawerActivity extends AppCompatActivity {

    private List<AppInfo> allApps = new ArrayList<>();
    private List<AppInfo> recentApps = new ArrayList<>();
    private AppListAdapter adapter;
    private SearchView searchView;
    private InputMethodManager imm;

    private float y1;
    private static final int MIN_DISTANCE = 150;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_drawer);

        RecyclerView allAppsRecycler = findViewById(R.id.all_apps_recycler);
        searchView = findViewById(R.id.search_view);
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        int plateId = searchView.getContext().getResources()
                .getIdentifier("android:id/search_plate", null, null);
        View searchPlate = searchView.findViewById(plateId);
        if (searchPlate != null) searchPlate.setBackgroundColor(Color.TRANSPARENT);

        requestUsageAccessPermission();

        new Thread(() -> {
            allApps = AppCache.allApps != null && !AppCache.allApps.isEmpty()
                    ? new ArrayList<>(AppCache.allApps)
                    : loadApps();

            runOnUiThread(() -> {
                SharedPreferences prefs = getSharedPreferences("saf_launcher_prefs", MODE_PRIVATE);
                int columnCount = prefs.getInt("grid_column_count", 5);
                int iconSize = prefs.getInt("grid_icon_size", 100);
                allAppsRecycler.setLayoutManager(new GridLayoutManager(this, columnCount));
                adapter = new AppListAdapter(this, allApps, iconSize);
                allAppsRecycler.setAdapter(adapter);
            });
        }).start();

        SharedPreferences prefs = getSharedPreferences("saf_launcher_prefs", MODE_PRIVATE);
        boolean showRecent = prefs.getBoolean("show_recent_apps", true);
        LinearLayout recentContainer = findViewById(R.id.recent_apps_container);
        View divider = findViewById(R.id.recent_divider);

        if (showRecent) {
            loadRecentApps();
            loadRecentAppsIntoView(recentContainer);
        }
        recentContainer.setVisibility(showRecent ? View.VISIBLE : View.GONE);
        if (divider != null) divider.setVisibility(showRecent ? View.VISIBLE : View.GONE);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_CONTACTS}, 101);
        }

        setupSearchView();
        focusSearchBar();
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }

            @Override public boolean onQueryTextChange(String newText) {
                if (adapter == null) return false; // 🔒 Prevent crash if adapter not ready

                String query = newText.toLowerCase(Locale.ROOT);
                adapter.filter(query);

                List<AppInfo> filtered = adapter.getFilteredList();
                findViewById(R.id.app_label).setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);

                if (filtered.size() == 1) {
                    showShortcutCard(filtered.get(0));
                } else {
                    hideDynamicViews();
                }

                boolean isEmpty = TextUtils.isEmpty(query);
                LinearLayout recentContainer = findViewById(R.id.recent_apps_container);
                View divider = ((ViewGroup) recentContainer.getParent()).findViewById(R.id.recent_divider);
                recentContainer.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                if (divider != null) divider.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

                if (!TextUtils.isEmpty(query)) {
                    updateContactResults(getMatchingContacts(query));
                } else {
                    updateContactResults(Collections.emptyList());
                }

                return true;
            }
        });
    }

    private void updateContactResults(List<ContactInfo> contacts) {
        LinearLayout layout = findViewById(R.id.contact_results);
        TextView label = findViewById(R.id.contact_label);
        layout.removeAllViews();

        boolean hasContacts = !contacts.isEmpty();
        layout.setVisibility(hasContacts ? View.VISIBLE : View.GONE);
        label.setVisibility(hasContacts ? View.VISIBLE : View.GONE);

        for (ContactInfo c : contacts) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(16, 16, 16, 16);
            row.setGravity(Gravity.CENTER_VERTICAL);

            ImageView icon = new ImageView(this);
            icon.setImageResource(android.R.drawable.ic_menu_call);
            icon.setLayoutParams(new LinearLayout.LayoutParams(60, 60));
            icon.setPadding(0, 0, 24, 0);

            TextView text = new TextView(this);
            text.setText(c.name + "   " + c.number);
            text.setTextColor(Color.WHITE);
            text.setTextSize(14);

            row.addView(icon);
            row.addView(text);
            row.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + c.number))));

            layout.addView(row);
        }
    }

    private void hideDynamicViews() {
        int[] ids = {R.id.shortcut_card, R.id.search_with_label, R.id.search_with_row,
                R.id.contact_label, R.id.contact_results, R.id.app_label};

        for (int id : ids) findViewById(id).setVisibility(View.GONE);
    }

    private List<AppInfo> loadApps() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        List<AppInfo> list = new ArrayList<>();

        for (ResolveInfo info : resolveInfos) {
            list.add(new AppInfo(info.loadLabel(pm).toString(), info.loadIcon(pm), info.activityInfo.packageName));
        }

        list.sort(Comparator.comparing(a -> a.label.toLowerCase(Locale.ROOT)));
        AppCache.allApps = new ArrayList<>(list);
        return list;
    }

    private void loadRecentApps() {
        recentApps.clear();
        UsageStatsManager usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        long now = System.currentTimeMillis();

        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 1000 * 60 * 60 * 24, now);
        if (stats == null || stats.isEmpty()) return;

        stats.sort((a, b) -> Long.compare(b.getLastTimeUsed(), a.getLastTimeUsed()));
        Set<String> added = new HashSet<>();

        for (UsageStats us : stats) {
            String pkg = us.getPackageName();
            if (added.contains(pkg)) continue;
            try {
                ApplicationInfo info = getPackageManager().getApplicationInfo(pkg, 0);
                if (getPackageManager().getLaunchIntentForPackage(pkg) != null) {
                    String label = getPackageManager().getApplicationLabel(info).toString();
                    Drawable icon = getPackageManager().getApplicationIcon(info);
                    recentApps.add(new AppInfo(label, icon, pkg));
                    added.add(pkg);
                }
            } catch (Exception ignored) {}
            if (recentApps.size() >= 8) break;
        }
    }

    private void loadRecentAppsIntoView(LinearLayout container) {
        container.removeAllViews();

        int iconSize = getSharedPreferences("saf_launcher_prefs", MODE_PRIVATE)
                .getInt("grid_icon_size", 100);
        int appsPerRow = 2;

        LinearLayout currentRow = null;
        for (int i = 0; i < recentApps.size(); i++) {
            if (i % appsPerRow == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setGravity(Gravity.CENTER_HORIZONTAL);
                currentRow.setPadding(0, 16, 0, 16);
                container.addView(currentRow);
            }

            AppInfo app = recentApps.get(i);
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);
            layout.setPadding(16, 8, 16, 8);

            ImageView icon = new ImageView(this);
            icon.setImageDrawable(app.icon);
            icon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));

            TextView label = new TextView(this);
            label.setText(app.label);
            label.setTextColor(Color.WHITE);
            label.setMaxLines(1);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setTextSize(12);
            label.setGravity(Gravity.CENTER);

            layout.addView(icon);
            layout.addView(label);
            layout.setOnClickListener(v -> {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(app.packageName);
                if (launchIntent != null) startActivity(launchIntent);
            });

            currentRow.addView(layout);
        }
    }

    private void showShortcutCard(AppInfo app) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;

        LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        UserHandle handle = android.os.Process.myUserHandle();
        List<ShortcutInfo> shortcuts;

        try {
            shortcuts = launcherApps.getShortcuts(
                    new LauncherApps.ShortcutQuery()
                            .setPackage(app.packageName)
                            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                                    | LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                                    | LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED),
                    handle);
        } catch (SecurityException e) {
            Log.e("SAFLauncher", "Shortcut permission denied", e);
            Toast.makeText(this, "Set as default launcher to enable shortcuts", Toast.LENGTH_LONG).show();
            return;
        }

        LinearLayout card = findViewById(R.id.shortcut_card);
        card.removeAllViews();

        if (shortcuts == null || shortcuts.isEmpty()) {
            card.setVisibility(View.GONE);
            return;
        }

        card.setVisibility(View.VISIBLE);

        for (ShortcutInfo s : shortcuts) {
            Drawable icon = launcherApps.getShortcutIconDrawable(s, getResources().getDisplayMetrics().densityDpi);
            CharSequence label = s.getShortLabel();

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(16, 12, 16, 12);
            row.setGravity(Gravity.CENTER_VERTICAL);

            ImageView iconView = new ImageView(this);
            if (icon != null) iconView.setImageDrawable(icon);

            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(60, 60);
            iconParams.setMargins(0, 0, 16, 0); // 👈 spacing between icon and label
            iconView.setLayoutParams(iconParams);


            TextView labelView = new TextView(this);
            labelView.setText(label);
            labelView.setTextColor(Color.WHITE);
            labelView.setTextSize(14);
            labelView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            row.addView(iconView);
            row.addView(labelView);

            row.setOnClickListener(v -> {
                try {
                    launcherApps.startShortcut(app.packageName, s.getId(), null, null, handle);
                } catch (Exception e) {
                    Log.e("SAFLauncher", "Failed to launch shortcut: " + s.getId(), e);
                    Toast.makeText(this, "Unable to launch shortcut", Toast.LENGTH_SHORT).show();
                }
            });

            card.addView(row);
        }
    }

    private void requestUsageAccessPermission() {
        if (!hasUsageAccessPermission()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Please allow Usage Access for recent apps", Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasUsageAccessPermission() {
        AppOpsManager ops = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void focusSearchBar() {
        if (searchView != null) {
            searchView.setIconified(false);
            searchView.requestFocus();
            if (imm != null) imm.showSoftInput(searchView.findFocus(), InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private List<ContactInfo> getMatchingContacts(String query) {
        List<ContactInfo> contacts = new ArrayList<>();
        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ? OR " +
                        ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ?",
                new String[]{"%" + query + "%", "%" + query + "%"},
                null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                contacts.add(new ContactInfo(cursor.getString(0), cursor.getString(1)));
            }
            cursor.close();
        }
        return contacts;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (searchView != null) {
            searchView.setQuery("", false);
            searchView.clearFocus();
            searchView.setIconified(false);
        }

        hideDynamicViews();

        SharedPreferences prefs = getSharedPreferences("saf_launcher_prefs", MODE_PRIVATE);
        boolean showRecent = prefs.getBoolean("show_recent_apps", true);

        LinearLayout recentContainer = findViewById(R.id.recent_apps_container);
        View divider = ((ViewGroup) recentContainer.getParent()).findViewById(R.id.recent_divider);
        recentContainer.setVisibility(showRecent ? View.VISIBLE : View.GONE);
        if (divider != null) divider.setVisibility(showRecent ? View.VISIBLE : View.GONE);

        focusSearchBar();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            y1 = event.getY();
        }
        return super.dispatchTouchEvent(event);
    }
}
