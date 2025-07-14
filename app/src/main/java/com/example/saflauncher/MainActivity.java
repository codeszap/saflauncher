package com.example.saflauncher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private PackageManager packageManager;
    private LauncherApps launcherApps;

    private GridView gridView;
    private SearchView searchView;

    private List<Item> allItems = new ArrayList<>();
    private List<Item> filteredItems = new ArrayList<>();
    private ItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        packageManager = getPackageManager();
        launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);

        gridView = findViewById(R.id.grid);
        searchView = findViewById(R.id.search_view);
        searchView.setIconified(false);
        searchView.requestFocus();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT);
        }

        loadAllItems();
        filteredItems.addAll(allItems);

        adapter = new ItemAdapter();
        gridView.setAdapter(adapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterItems(newText);
                return true;
            }
        });

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            Item item = filteredItems.get(position);
            if (item.isShortcut) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                        launcherApps.startShortcut(item.packageName, item.shortcutId, null, null, Process.myUserHandle());
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to launch shortcut", Toast.LENGTH_SHORT).show();
                }
            } else {
                Intent intent = packageManager.getLaunchIntentForPackage(item.packageName);
                if (intent != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Cannot launch app", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadAllItems() {
        allItems.clear();

        // Load all launchable apps
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = packageManager.queryIntentActivities(intent, 0);

        for (ResolveInfo app : apps) {
            Drawable icon = app.loadIcon(packageManager);
            String label = app.loadLabel(packageManager).toString();
            String packageName = app.activityInfo.packageName;
            allItems.add(new Item(label, icon, packageName));
        }

        // Load shortcuts (Android 7.1+ only)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            for (ResolveInfo app : apps) {
                try {
                    String pkg = app.activityInfo.packageName;

                    LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
                    query.setPackage(pkg);
                    query.setQueryFlags(
                            LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC |
                                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                    );

                    List<ShortcutInfo> shortcuts = launcherApps.getShortcuts(query, Process.myUserHandle());

                    if (shortcuts != null) {
                        for (ShortcutInfo si : shortcuts) {
                            CharSequence shortLabel = si.getShortLabel();
                            Drawable icon = launcherApps.getShortcutIconDrawable(si, 0);

                            // 🔍 Log shortcut info
                            Log.d("SHORTCUT", "Loaded shortcut: " + shortLabel + " from " + pkg);

                            allItems.add(new Item(shortLabel.toString(), icon, pkg, si.getId()));
                        }
                    } else {
                        Log.w("SHORTCUT", "No shortcuts found for package: " + pkg);
                    }

                } catch (Exception e) {
                    Log.e("SHORTCUT", "Failed loading shortcut for package: " + app.activityInfo.packageName, e);
                }
            }

            // ✅ Check permission
            if (!launcherApps.hasShortcutHostPermission()) {
                Toast.makeText(this, "Shortcut permission not granted", Toast.LENGTH_LONG).show();
                Log.w("SHORTCUT", "No shortcut host permission!");
            }
        } else {
            Log.w("SHORTCUT", "Shortcuts not supported below Android 7.1");
        }
    }

    private void filterItems(String query) {
        filteredItems.clear();
        query = query.toLowerCase();

        for (Item item : allItems) {
            if (!item.isShortcut && item.label.toLowerCase().contains(query)) {
                // Add main app
                filteredItems.add(item);

                // Add its shortcuts
                for (Item shortcut : allItems) {
                    if (shortcut.isShortcut && shortcut.packageName.equals(item.packageName)) {
                        filteredItems.add(shortcut);
                    }
                }
            }
        }

        adapter.notifyDataSetChanged();
    }


    private class ItemAdapter extends BaseAdapter {
        @Override public int getCount() { return filteredItems.size(); }
        @Override public Object getItem(int i) { return filteredItems.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int i, View convertView, ViewGroup parent) {
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.app_item, parent, false);
            ImageView icon = view.findViewById(R.id.icon);
            TextView label = view.findViewById(R.id.label);

            Item item = filteredItems.get(i);
            icon.setImageDrawable(item.icon);
            label.setText(item.label);

            return view;
        }
    }

    private static class Item {
        String label;
        Drawable icon;
        String packageName;
        boolean isShortcut = false;
        String shortcutId = null;

        Item(String label, Drawable icon, String packageName) {
            this.label = label;
            this.icon = icon;
            this.packageName = packageName;
        }

        Item(String label, Drawable icon, String packageName, String shortcutId) {
            this(label, icon, packageName);
            this.shortcutId = shortcutId;
            this.isShortcut = true;
        }
    }
}
