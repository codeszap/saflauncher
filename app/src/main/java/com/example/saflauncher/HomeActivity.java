package com.example.saflauncher;

import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.pm.PackageManager;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        rootLayout = findViewById(R.id.home_root);
//        ImageView btnDrawer = findViewById(R.id.btn_drawer);
//
//        btnDrawer.setOnClickListener(v -> openDrawer());

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

        checkStoragePermission(); // Load wallpaper once at startup
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
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(30); // Deprecated but still works in older Android
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
                wallpaperDrawable = wallpaperManager.getDrawable(); // fallback
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
            overridePendingTransition(0, 0); // Fastest transition
        });
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                y1 = event.getY();
                touchDownTime = System.currentTimeMillis();
                drawerOpened = false;
                break;

            case MotionEvent.ACTION_UP:
                y2 = event.getY();
                long pressDuration = System.currentTimeMillis() - touchDownTime;

                if (pressDuration > LONG_PRESS_TIME) {
                    showHomeOptionsMenu();
                    return true;
                }

                if (!drawerOpened && (y1 - y2 > MIN_DISTANCE)) {
                    drawerOpened = true;
                    vibrate();
                    openDrawer();
                    return true;
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    private void showHomeOptionsMenu() {
        String[] options = {"Change Wallpaper", "Launcher Settings", "Add Widget"};

        new AlertDialog.Builder(this)
                .setTitle("Home Options")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: openWallpaperPicker(); break;
                        case 1: Toast.makeText(this, "Settings Coming Soon!", Toast.LENGTH_SHORT).show(); break;
                        case 2: Toast.makeText(this, "Add Widget Coming Soon!", Toast.LENGTH_SHORT).show(); break;
                    }
                }).show();
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
}
