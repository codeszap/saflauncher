package com.example.saflauncher;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import java.util.*;

public class AppDrawerActivity extends AppCompatActivity {

    private List<AppInfo> allApps = new ArrayList<>();
    private List<AppInfo> recentApps = new ArrayList<>();
    private AppListAdapter adapter;
    private GridView appGrid;
    private SearchView searchView;
    private InputMethodManager imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_drawer);

        appGrid = findViewById(R.id.app_grid);
        searchView = findViewById(R.id.search_view);
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        // Remove underline bg
        int plateId = searchView.getContext().getResources()
                .getIdentifier("android:id/search_plate", null, null);
        View searchPlate = searchView.findViewById(plateId);
        if (searchPlate != null) {
            searchPlate.setBackgroundColor(Color.TRANSPARENT);
        }

        // Permission
        requestUsageAccessPermission();

        // Load data
        loadApps();
        loadRecentApps();

        adapter = new AppListAdapter(this, allApps);
        appGrid.setAdapter(adapter);

        // All Apps click
        appGrid.setOnItemClickListener((parent, view, position, id) -> {
            AppInfo app = (AppInfo) adapter.getItem(position);
            if (app != null) {
                Intent intent = getPackageManager().getLaunchIntentForPackage(app.packageName);
                if (intent != null) startActivity(intent);
            }
        });

        // Search filtering
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText.toLowerCase(Locale.ROOT));
                return true;
            }
        });

        // Load recent into horizontal row
        LinearLayout recentContainer = findViewById(R.id.recent_apps_container);
        loadRecentAppsIntoView(recentContainer);

        focusSearchBar();
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo info : apps) {
            String label = info.loadLabel(pm).toString();
            String packageName = info.activityInfo.packageName;
            Drawable icon = info.loadIcon(pm);
            allApps.add(new AppInfo(label, icon, packageName));
        }
    }

    private void loadRecentApps() {
        recentApps.clear();
        UsageStatsManager usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        long now = System.currentTimeMillis();

        List<UsageStats> stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - 1000 * 60 * 60 * 24,
                now
        );

        if (stats == null || stats.isEmpty()) return;

        Collections.sort(stats, (a, b) -> Long.compare(b.getLastTimeUsed(), a.getLastTimeUsed()));
        Set<String> added = new HashSet<>();

        for (UsageStats us : stats) {
            String pkg = us.getPackageName();
            if (added.contains(pkg)) continue;

            try {
                ApplicationInfo appInfo = getPackageManager().getApplicationInfo(pkg, 0);
                if (getPackageManager().getLaunchIntentForPackage(pkg) != null) {
                    String label = getPackageManager().getApplicationLabel(appInfo).toString();
                    Drawable icon = getPackageManager().getApplicationIcon(appInfo);
                    recentApps.add(new AppInfo(label, icon, pkg));
                    added.add(pkg);
                }
            } catch (Exception ignored) {}

            if (recentApps.size() >= 8) break;
        }
    }

    private void loadRecentAppsIntoView(LinearLayout container) {
        container.removeAllViews();

        for (AppInfo app : recentApps) {
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setPadding(16, 8, 16, 8);
            itemLayout.setGravity(Gravity.CENTER);

            ImageView iconView = new ImageView(this);
            iconView.setImageDrawable(app.icon);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(100, 100);
            iconView.setLayoutParams(iconParams);

            TextView labelView = new TextView(this);
            labelView.setText(app.label);
            labelView.setTextColor(Color.WHITE);
            labelView.setMaxLines(1);
            labelView.setEllipsize(TextUtils.TruncateAt.END);
            labelView.setTextSize(12);
            labelView.setGravity(Gravity.CENTER);

            itemLayout.addView(iconView);
            itemLayout.addView(labelView);

            itemLayout.setOnClickListener(v -> {
                Intent intent = getPackageManager().getLaunchIntentForPackage(app.packageName);
                if (intent != null) startActivity(intent);
            });

            container.addView(itemLayout);
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
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void focusSearchBar() {
        if (searchView != null) {
            searchView.setIconified(false);
            searchView.requestFocus();
            if (imm != null) {
                imm.showSoftInput(searchView.findFocus(), InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        focusSearchBar();
    }
}
