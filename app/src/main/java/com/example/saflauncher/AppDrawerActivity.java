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
import android.os.Handler;
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

import androidx.annotation.RequiresApi;
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

    private float y1, y2;
    private final int MIN_DISTANCE = 150; // Minimum distance to detect swipe


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
        if (searchPlate != null) {
            searchPlate.setBackgroundColor(Color.TRANSPARENT);
        }

        requestUsageAccessPermission();

        // 🧠 Load all apps in background to avoid UI freeze
        new Thread(() -> {
            loadApps();

            runOnUiThread(() -> {
                SharedPreferences prefs = getSharedPreferences("saf_launcher_prefs", MODE_PRIVATE);
                int columnCount = prefs.getInt("grid_column_count", 5);
                int iconSize = prefs.getInt("grid_icon_size", 100); // default size

                allAppsRecycler.setLayoutManager(new GridLayoutManager(AppDrawerActivity.this, columnCount));

// Pass iconSize to adapter
                adapter = new AppListAdapter(AppDrawerActivity.this, allApps, iconSize);
                allAppsRecycler.setAdapter(adapter);

            });
        }).start();

        // 🕒 Delay recent apps load slightly to avoid initial UI blocking
        new Handler().postDelayed(() -> {
            loadRecentApps();
            LinearLayout recentContainer = findViewById(R.id.recent_apps_container);
            loadRecentAppsIntoView(recentContainer);
        }, 300);

        // 🔐 Ask contact permission if not given
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_CONTACTS},
                    101);
        }

        // 🔍 Setup search bar
        setupSearchView();

        // 🎯 Focus search bar & load empty recent container first
        LinearLayout recentContainer = findViewById(R.id.recent_apps_container);
        recentContainer.removeAllViews();
        focusSearchBar();
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null)
                    adapter.filter(newText.toLowerCase(Locale.ROOT));

                TextView appLabel = findViewById(R.id.app_label);

                if (adapter != null) {
                    List<AppInfo> filtered = adapter.getFilteredList();
                    appLabel.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);

                    if (filtered.size() == 1) {
                        showShortcutCard(filtered.get(0));
                    } else {
                        hideDynamicViews();
                    }
                }


                LinearLayout recentContainer = findViewById(R.id.recent_apps_container);
                View divider = ((ViewGroup) recentContainer.getParent()).findViewById(R.id.recent_divider);

                boolean empty = TextUtils.isEmpty(newText.trim());
                recentContainer.setVisibility(empty ? View.VISIBLE : View.GONE);
                if (divider != null) divider.setVisibility(empty ? View.VISIBLE : View.GONE);

                if (adapter != null) {
                    List<AppInfo> filtered = adapter.getFilteredList();
                    if (filtered.size() == 1) {
                        showShortcutCard(filtered.get(0));
                    } else {
                        hideDynamicViews();
                    }
                }

                List<ContactInfo> matchingContacts = getMatchingContacts(newText);
                updateContactResults(matchingContacts);
                return true;
            }
        });
    }

    private void updateContactResults(List<ContactInfo> contacts) {
        LinearLayout contactLayout = findViewById(R.id.contact_results);
        TextView contactLabel = findViewById(R.id.contact_label);

        contactLayout.removeAllViews();

        if (!contacts.isEmpty()) {
            contactLayout.setVisibility(View.VISIBLE);
            contactLabel.setVisibility(View.VISIBLE);

            for (ContactInfo c : contacts) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(16, 16, 16, 16);

                ImageView icon = new ImageView(this);
                icon.setImageResource(android.R.drawable.ic_menu_call);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(60, 60);
                iconParams.setMargins(0, 0, 24, 0);
                icon.setLayoutParams(iconParams);

                TextView tv = new TextView(this);
                tv.setText(c.name + "   " + c.number);
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(14);

                row.addView(icon);
                row.addView(tv);

                row.setOnClickListener(v -> {
                    Intent i = new Intent(Intent.ACTION_DIAL);
                    i.setData(Uri.parse("tel:" + c.number));
                    startActivity(i);
                });

                contactLayout.addView(row);
            }
        } else {
            contactLayout.setVisibility(View.GONE);
            contactLabel.setVisibility(View.GONE);
        }
    }

    private void hideDynamicViews() {
        findViewById(R.id.shortcut_card).setVisibility(View.GONE);
        findViewById(R.id.search_with_label).setVisibility(View.GONE);
        findViewById(R.id.search_with_row).setVisibility(View.GONE);
        findViewById(R.id.contact_label).setVisibility(View.GONE);
        findViewById(R.id.contact_results).setVisibility(View.GONE);
        findViewById(R.id.app_label).setVisibility(View.GONE);
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

        // Sort A-Z
        Collections.sort(allApps, Comparator.comparing(app -> app.label.toLowerCase(Locale.ROOT)));
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

        SharedPreferences prefs = getSharedPreferences("saf_launcher_prefs", MODE_PRIVATE);
        int iconSize = prefs.getInt("grid_icon_size", 100);
        int appsPerRow = 2; // 👉 Adjust this for 3 or 4 per row

        LinearLayout currentRow = null;

        for (int i = 0; i < recentApps.size(); i++) {
            if (i % appsPerRow == 0) {
                // 🔃 New row
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setGravity(Gravity.CENTER_HORIZONTAL);
                currentRow.setPadding(0, 16, 0, 16);
                container.addView(currentRow);
            }

            AppInfo app = recentApps.get(i);

            // 📦 App item
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setPadding(16, 8, 16, 8);
            itemLayout.setGravity(Gravity.CENTER);

            ImageView iconView = new ImageView(this);
            iconView.setImageDrawable(app.icon);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
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

            // 🔄 Add to current row
            currentRow.addView(itemLayout);
        }
    }

    private void showShortcutCard(AppInfo app) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;

        LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
        UserHandle userHandle = android.os.Process.myUserHandle();
        List<ShortcutInfo> shortcuts = new ArrayList<>();

        try {
            shortcuts = launcherApps.getShortcuts(
                    new LauncherApps.ShortcutQuery()
                            .setPackage(app.packageName)
                            .setQueryFlags(
                                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC |
                                            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST |
                                            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                            ),
                    userHandle
            );
        } catch (SecurityException e) {
            Log.e("SAFLauncher", "No permission for shortcuts", e);
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
            CharSequence label = s.getShortLabel();
            Drawable icon = launcherApps.getShortcutIconDrawable(s, getResources().getDisplayMetrics().densityDpi);

            if (label != null) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(16, 12, 16, 12);

                ImageView iconView = new ImageView(this);
                if (icon != null) {
                    iconView.setImageDrawable(icon);
                }
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(60, 60);
                iconParams.setMargins(0, 0, 16, 0);
                iconView.setLayoutParams(iconParams);

                TextView labelView = new TextView(this);
                labelView.setText(label);
                labelView.setTextColor(Color.WHITE);
                labelView.setTextSize(14);
                labelView.setGravity(Gravity.START);
                labelView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                ));

                row.addView(iconView);
                row.addView(labelView);

                // 💥 Launch shortcut using startShortcut
                row.setOnClickListener(v -> {
                    try {
                        launcherApps.startShortcut(
                                app.packageName,
                                s.getId(),
                                null,
                                null,
                                userHandle
                        );
                    } catch (Exception e) {
                        Log.e("SAFLauncher", "Failed to launch shortcut: " + s.getId(), e);
                        Toast.makeText(this, "Unable to launch shortcut", Toast.LENGTH_SHORT).show();
                    }
                });

                card.addView(row);
            }
        }
    }

    private void showSearchWithRow(String query) {
        LinearLayout row = findViewById(R.id.search_with_row);
        TextView label = findViewById(R.id.search_with_label);
        row.removeAllViews();

        String[] searchTargets = {"Google", "Play", "YouTube", "Wikipedia"};
        int[] icons = {
//                R.drawable.ic_google, R.drawable.ic_playstore,
//                R.drawable.ic_youtube, R.drawable.ic_wikipedia
        };
        String[] urls = {
                "https://www.google.com/search?q=",
                "https://play.google.com/store/search?q=",
                "https://www.youtube.com/results?search_query=",
                "https://en.wikipedia.org/wiki/Special:Search/"
        };

        for (int i = 0; i < searchTargets.length; i++) {
            ImageView icon = new ImageView(this);
            // icon.setImageResource(icons[i]); // Enable when you have drawable icons
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 100);
            params.setMargins(12, 0, 12, 0);
            icon.setLayoutParams(params);
            int finalI = i;

            icon.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(android.net.Uri.parse(urls[finalI] + query));
                startActivity(intent);
            });

            row.addView(icon);
        }

        label.setVisibility(View.VISIBLE);
        row.setVisibility(View.VISIBLE);
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

    private List<ContactInfo> getMatchingContacts(String query) {
        List<ContactInfo> contacts = new ArrayList<>();
        ContentResolver cr = getContentResolver();

        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };

        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ? OR " +
                ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ?";
        String[] selectionArgs = { "%" + query + "%", "%" + query + "%" };

        Cursor cursor = cr.query(uri, projection, selection, selectionArgs, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                String number = cursor.getString(1);
                contacts.add(new ContactInfo(name, number));
            }
            cursor.close();
        }

        return contacts;
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Clear search text
        if (searchView != null) {
            searchView.setQuery("", false); // clear text
            searchView.clearFocus(); // just to reset
            searchView.setIconified(false); // force focus
        }

        // Hide shortcut card & others
        findViewById(R.id.shortcut_card).setVisibility(View.GONE);
        findViewById(R.id.search_with_label).setVisibility(View.GONE);
        findViewById(R.id.search_with_row).setVisibility(View.GONE);
        findViewById(R.id.contact_results).setVisibility(View.GONE);
        findViewById(R.id.contact_label).setVisibility(View.GONE);
        findViewById(R.id.app_label).setVisibility(View.GONE);


        // Show recent apps + divider again
        LinearLayout recentContainer = findViewById(R.id.recent_apps_container);
        View divider = ((ViewGroup) recentContainer.getParent()).findViewById(R.id.recent_divider);
        recentContainer.setVisibility(View.VISIBLE);
        if (divider != null) divider.setVisibility(View.VISIBLE);

        // Refocus the search bar
        focusSearchBar();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                y1 = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                y2 = event.getY();
                if (y2 - y1 > MIN_DISTANCE) {
                    // Swipe down detected
                    finish(); // 👈 Close the AppDrawer
                    overridePendingTransition(0, 0);
                    return true;
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }


}


