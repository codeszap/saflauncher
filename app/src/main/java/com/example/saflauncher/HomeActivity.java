package com.example.saflauncher;

import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.VibrationEffect;
import android.provider.Settings;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.Manifest;
import android.os.Vibrator;

public class HomeActivity extends AppCompatActivity {

    private float y1, y2;
    private long touchDownTime = 0;
    private final int MIN_DISTANCE = 150;
    private final long LONG_PRESS_TIME = 500;
    private boolean drawerOpened = false;
    private RelativeLayout rootLayout;

    private static final int REQUEST_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;

    private DevicePolicyManager devicePolicyManager;
    private ComponentName compName;
    private long lastTapTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        rootLayout = findViewById(R.id.home_root);

        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        compName = new ComponentName(this, MyDeviceAdminReceiver.class);

        preloadAppList();

        if (!devicePolicyManager.isAdminActive(compName)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Double tap to lock screen needs admin permission.");
            startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
        }

        rootLayout.setOnDragListener((view, dragEvent) -> {
            switch (dragEvent.getAction()) {
                case DragEvent.ACTION_DROP:
                    AppInfo droppedApp = (AppInfo) dragEvent.getLocalState();
                    float x = dragEvent.getX();
                    float y = dragEvent.getY();
                    addShortcutToHome(droppedApp, x, y);
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    rootLayout.setAlpha(0.8f);
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    rootLayout.setAlpha(1f);
                    return true;
            }
            return true;
        });

        checkStoragePermission();

        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "⚠️ Enable Accessibility for Notification Swipe", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        }
    }

    private boolean isDefaultLauncher() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        PackageManager pm = getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo != null && resolveInfo.activityInfo.packageName.equals(getPackageName());
    }

    private void promptSetAsDefaultLauncher() {
        new AlertDialog.Builder(this)
                .setTitle("Set SAF Launcher as Default")
                .setMessage("Please set SAF Launcher as the default home app.")
                .setPositiveButton("Set Now", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            loadWallpaper();
        }
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(30);
            }
        }
    }

    private void loadWallpaper() {
        try {
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
            Drawable wallpaperDrawable = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ParcelFileDescriptor pfd = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM);
                if (pfd != null) {
                    FileInputStream inputStream = new FileInputStream(pfd.getFileDescriptor());
                    wallpaperDrawable = Drawable.createFromStream(inputStream, null);
                    inputStream.close();
                    pfd.close();
                }
            }

            if (wallpaperDrawable == null) {
                wallpaperDrawable = wallpaperManager.getDrawable();
            }

            if (wallpaperDrawable != null) {
                rootLayout.setBackground(wallpaperDrawable);
            } else {
                Toast.makeText(this, "Wallpaper not found", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading wallpaper", Toast.LENGTH_SHORT).show();
        }
    }

    private void openDrawer() {
        rootLayout.post(() -> {
            Intent intent = new Intent(this, AppDrawerActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0); // ⚡ removes animation delay
        });
    }


    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        long currentTime = System.currentTimeMillis();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (currentTime - lastTapTime < 300) {
                lockScreen();
                return true;
            }
            lastTapTime = currentTime;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                y1 = event.getY();
                touchDownTime = currentTime;
                drawerOpened = false;
                break;

            case MotionEvent.ACTION_UP:
                y2 = event.getY();
                long pressDuration = currentTime - touchDownTime;

                if (pressDuration > LONG_PRESS_TIME) {
                    vibrate();
                    showHomeOptionsMenu();
                    return true;
                }

                if (!drawerOpened && (y1 - y2 > MIN_DISTANCE)) {
                    drawerOpened = true;
                    vibrate();
                    openDrawer();
                    return true;
                }

                if (y2 - y1 > MIN_DISTANCE) {
                    if (MyAccessibilityService.instance != null) {
                        MyAccessibilityService.instance.triggerSwipeDown();
                    } else {
                        Toast.makeText(this, "Accessibility Not Enabled!", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                } else if (y1 - y2 > MIN_DISTANCE) {
                    drawerOpened = true;
                    vibrate();
                    openDrawer();
                    return true;
                }

                break;
        }

        return super.dispatchTouchEvent(event);
    }

//    private void preloadAppList() {
//        new Thread(() -> {
//            PackageManager pm = getPackageManager();
//            Intent intent = new Intent(Intent.ACTION_MAIN, null);
//            intent.addCategory(Intent.CATEGORY_LAUNCHER);
//            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
//
//            List<AppInfo> apps = new java.util.ArrayList<>();
//            for (ResolveInfo resolveInfo : resolveInfos) {
//                AppInfo appInfo = new AppInfo();
//                appInfo.label = resolveInfo.loadLabel(pm).toString();
//                appInfo.packageName = resolveInfo.activityInfo.packageName;
//                appInfo.icon = resolveInfo.loadIcon(pm);
//                apps.add(appInfo);
//            }
//
//            AppCache.allApps = apps;
//        }).start();
//    }


    private void preloadAppList() {
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);

            List<AppInfo> apps = new ArrayList<>();
            for (ResolveInfo resolveInfo : resolveInfos) {
                AppInfo appInfo = new AppInfo();
                appInfo.label = resolveInfo.loadLabel(pm).toString();
                appInfo.packageName = resolveInfo.activityInfo.packageName;
                appInfo.icon = resolveInfo.loadIcon(pm);
                apps.add(appInfo);
            }

            // ✅ Usage stats map
            Map<String, Long> usageStats = getAppUsageStats();

            // 🔀 Sort based on usage descending, fallback to A-Z if usage data not found
            apps.sort((a1, a2) -> {
                long usage1 = usageStats.getOrDefault(a1.packageName, 0L);
                long usage2 = usageStats.getOrDefault(a2.packageName, 0L);
                if (usage1 == usage2) {
                    return a1.label.compareToIgnoreCase(a2.label);
                }
                return Long.compare(usage2, usage1); // High usage first
            });

            AppCache.allApps = apps;
        }).start();
    }

    private Map<String, Long> getAppUsageStats() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        long endTime = System.currentTimeMillis();
        long beginTime = endTime - (1000L * 60 * 60 * 24 * 7); // Last 7 days

        List<UsageStats> stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime);

        Map<String, Long> usageMap = new HashMap<>();
        if (stats != null) {
            for (UsageStats usage : stats) {
                usageMap.put(usage.getPackageName(), usage.getTotalTimeInForeground());
            }
        }
        return usageMap;
    }


    private void lockScreen() {
        if (devicePolicyManager.isAdminActive(compName)) {
            devicePolicyManager.lockNow();
        } else {
            Toast.makeText(this, "Please enable device admin", Toast.LENGTH_SHORT).show();
        }
    }

    private void showHomeOptionsMenu() {
        String[] options = {"Change Wallpaper", "Launcher Settings", "Add Widget"};
        new AlertDialog.Builder(this)
                .setTitle("Home Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: openWallpaperPicker(); break;
                        case 1:
                            Intent settingsIntent = new Intent(this, LauncherSettingsActivity.class);
                            startActivity(settingsIntent);
                            break;
                        case 2:
                            Toast.makeText(this, "Add Widget Coming Soon!", Toast.LENGTH_SHORT).show(); break;
                    }
                }).show();
    }

    private void expandNotificationBar() {
        if (MyAccessibilityService.instance != null) {
            MyAccessibilityService.instance.triggerSwipeDown();
            Toast.makeText(this, "📥 Notification Panel Opening...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "⚠️ Enable Accessibility Service First", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        String service = getPackageName() + "/" + MyAccessibilityService.class.getName();
        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        return enabledServices != null && enabledServices.contains(service);
    }

    private void openWallpaperPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
            startActivity(Intent.createChooser(intent, "Choose Wallpaper"));
        } catch (Exception e) {
            Toast.makeText(this, "Wallpaper picker not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void addShortcutToHome(AppInfo app, float x, float y) {
        ImageView shortcut = new ImageView(this);
        shortcut.setImageDrawable(app.icon);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(140, 140);
        params.leftMargin = (int) x - 70;
        params.topMargin = (int) y - 70;
        shortcut.setLayoutParams(params);

        shortcut.setOnClickListener(v -> {
            Intent intent = getPackageManager().getLaunchIntentForPackage(app.packageName);
            if (intent != null) startActivity(intent);
        });

        shortcut.setOnLongClickListener(v -> {
            showShortcutOptions(app, shortcut);
            return true;
        });

        rootLayout.addView(shortcut);
    }

    private void showShortcutOptions(AppInfo app, View shortcutView) {
        String[] options = {"App Info", "Uninstall", "Remove from Home"};
        new AlertDialog.Builder(this)
                .setTitle(app.label)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Intent infoIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            infoIntent.setData(Uri.parse("package:" + app.packageName));
                            startActivity(infoIntent);
                            break;
                        case 1:
                            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE);
                            uninstallIntent.setData(Uri.parse("package:" + app.packageName));
                            startActivity(uninstallIntent);
                            break;
                        case 2:
                            rootLayout.removeView(shortcutView);
                            break;
                    }
                }).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadWallpaper();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        // 🔁 ADD THIS LINE — refresh app list every time screen resumes
//        preloadAppList();
//
//        // ✅ Prompt to set as default launcher if not already
//        if (!isDefaultLauncher()) {
//            promptSetAsDefaultLauncher();
//        }
//
//        if (isAccessibilityServiceEnabled()) {
//            if (MyAccessibilityService.instance == null) {
//                Toast.makeText(this, "🔄 Trying to reconnect Accessibility...", Toast.LENGTH_SHORT).show();
//            }
//        } else {
//            Toast.makeText(this, "⚠️ Accessibility Disabled. Please enable", Toast.LENGTH_LONG).show();
//            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
//            startActivity(intent);
//        }
//    }

    @Override
    protected void onResume() {
        super.onResume();

        // 🔁 Already existing code
        preloadAppList();

        if (!isDefaultLauncher()) {
            promptSetAsDefaultLauncher();
        }

        if (isAccessibilityServiceEnabled()) {
            if (MyAccessibilityService.instance == null) {
                Toast.makeText(this, "🔄 Trying to reconnect Accessibility...", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "⚠️ Accessibility Disabled. Please enable", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        }

        // ✅ 👉 Add this below all existing code
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                startService(new Intent(this, EdgeGestureService.class));
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

}
