package com.example.saflauncher;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppDrawerActivity extends AppCompatActivity {

    private List<AppInfo> allApps = new ArrayList<>();
    private AppListAdapter adapter;
    private GridView appGrid;
    private String lastShortcutShownForPackage = "";
    private SearchView searchView;
    private InputMethodManager imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_drawer);

        appGrid = findViewById(R.id.app_grid);
        searchView = findViewById(R.id.search_view);
        LinearLayout shortcutCard = findViewById(R.id.shortcut_card);
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        loadApps();

        adapter = new AppListAdapter(this, allApps);
        appGrid.setAdapter(adapter);

        appGrid.setOnItemClickListener((parent, view, position, id) -> {
            AppInfo selectedApp = (AppInfo) adapter.getItem(position);
            if (selectedApp != null) {
                Toast.makeText(AppDrawerActivity.this, "Opening: " + selectedApp.label, Toast.LENGTH_SHORT).show();

                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(selectedApp.packageName);
                if (launchIntent != null) startActivity(launchIntent);

                // Shortcut card for API >= 25
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    showShortcutCardForApp(selectedApp);
                }

                // 🔥 Re-focus search bar after app click
                focusSearchBar();
            }
        });



        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText.toLowerCase(Locale.ROOT));

                List<AppInfo> filtered = adapter.getFilteredList();
                if (filtered.size() == 1) {
                    AppInfo matchedApp = filtered.get(0);

                    if (!matchedApp.packageName.equals(lastShortcutShownForPackage)) {
                        lastShortcutShownForPackage = matchedApp.packageName;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                            List<ShortcutInfo> shortcuts = getShortcutsForPackage(matchedApp.packageName);
                            List<ShortcutInfoModel> models = new ArrayList<>();
                            LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);

                            for (ShortcutInfo s : shortcuts) {
                                CharSequence shortLabel = s.getShortLabel();
                                Intent intent = s.getIntent();

                                if (intent == null) {
                                    Intent[] intents = null;
                                    try {
                                        intents = s.getIntents();
                                    } catch (Exception e) {
                                        intents = null;
                                    }

                                    if (intents != null && intents.length > 0) {
                                        intent = intents[0];
                                    }
                                }

                                Drawable icon = null;
                                if (launcherApps != null) {
                                    icon = launcherApps.getShortcutIconDrawable(s, getResources().getDisplayMetrics().densityDpi);
                                }

                                if (shortLabel != null) {
                                    models.add(new ShortcutInfoModel(matchedApp.label, shortLabel, icon, intent));
                                    Log.d("SAFDebug", "Shortcut: " + shortLabel + " | HasIntent: " + (intent != null));
                                }
                            }

                            showShortcutCard(models);
                        }
                    }
                } else {
                    lastShortcutShownForPackage = "";
                    findViewById(R.id.shortcut_card).setVisibility(View.GONE);

                    // 🔥 No match → refocus search
                    focusSearchBar();
                }

                return true;
            }
        });

        // 🔥 Auto focus search on launch
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
            AppInfo app = new AppInfo(label, info.loadIcon(pm), packageName);
            allApps.add(app);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 🔥 Clear the search text
        if (searchView != null) {
            searchView.setQuery("", false);
            searchView.clearFocus();
        }

        // 🔥 Hide shortcut card
        findViewById(R.id.shortcut_card).setVisibility(View.GONE);

        // 🔥 Re-focus search bar with keyboard
        focusSearchBar();
    }


    private void showShortcutCardForApp(AppInfo app) {
        List<ShortcutInfo> shortcuts = getShortcutsForPackage(app.packageName);
        List<ShortcutInfoModel> models = new ArrayList<>();
        LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);

        for (ShortcutInfo s : shortcuts) {
            CharSequence shortLabel = s.getShortLabel();
            Intent intent = s.getIntent();

            if (intent == null) {
                Intent[] intents = s.getIntents();
                if (intents != null && intents.length > 0) {
                    intent = intents[0];
                }
            }

            Drawable icon = null;
            if (launcherApps != null) {
                icon = launcherApps.getShortcutIconDrawable(s, getResources().getDisplayMetrics().densityDpi);
            }

            if (shortLabel != null) {
                models.add(new ShortcutInfoModel(app.label, shortLabel, icon, intent));
            }
        }

        showShortcutCard(models);
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private List<ShortcutInfo> getShortcutsForPackage(String packageName) {
        List<ShortcutInfo> result = new ArrayList<>();
        LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);

        if (launcherApps != null) {
            List<UserHandle> profiles = launcherApps.getProfiles();
            for (UserHandle profile : profiles) {
                List<ShortcutInfo> shortcuts = launcherApps.getShortcuts(
                        new LauncherApps.ShortcutQuery()
                                .setPackage(packageName)
                                .setQueryFlags(
                                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC |
                                                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST |
                                                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                                ),
                        profile
                );

                Log.d("SAFDebug", "Shortcuts found: " + (shortcuts != null ? shortcuts.size() : 0));

                if (shortcuts != null) {
                    for (ShortcutInfo s : shortcuts) {
                        Log.d("SAFDebug", "Shortcut: " + s.getShortLabel());
                        result.add(s);
                    }
                }
            }
        } else {
            Log.d("SAFDebug", "LauncherApps null");
        }

        return result;
    }

    private void showShortcutCard(List<ShortcutInfoModel> shortcuts) {
        LinearLayout cardLayout = findViewById(R.id.shortcut_card);
        cardLayout.removeAllViews();

        if (shortcuts.isEmpty()) {
            cardLayout.setVisibility(View.GONE);
            return;
        }

        cardLayout.setVisibility(View.VISIBLE);

        for (ShortcutInfoModel shortcut : shortcuts) {
            Button btn = new Button(this);
            btn.setText(shortcut.shortLabel);
            if (shortcut.icon != null) {
                btn.setCompoundDrawablesWithIntrinsicBounds(null, shortcut.icon, null, null);
            }

            btn.setOnClickListener(v -> {
                if (shortcut.intent != null) {
                    startActivity(shortcut.intent);
                } else {
                    Toast.makeText(this, "This shortcut can't be opened.", Toast.LENGTH_SHORT).show();
                }
            });

            btn.setEnabled(shortcut.intent != null);
            btn.setAlpha(shortcut.intent != null ? 1f : 0.5f);

            cardLayout.addView(btn);
        }
    }

    private void focusSearchBar() {
        if (searchView != null) {
            searchView.setIconified(false); // Expand if collapsed
            searchView.requestFocus();

            if (imm != null) {
                imm.showSoftInput(searchView.findFocus(), InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }
}
