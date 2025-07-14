package com.example.saflauncher;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

public class AppListAdapter extends BaseAdapter {

    private final Context context;
    private final List<AppInfo> originalApps;
    private final List<AppInfo> filteredApps;

    public AppListAdapter(Context context, List<AppInfo> apps) {
        this.context = context;
        this.originalApps = new ArrayList<>(apps);
        this.filteredApps = new ArrayList<>(apps);
    }

    @Override
    public int getCount() {
        return filteredApps.size();
    }

    @Override
    public Object getItem(int position) {
        return filteredApps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    public List<AppInfo> getFilteredList() {
        return filteredApps;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AppInfo app = filteredApps.get(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_app_icon, parent, false);
        }

        ImageView icon = convertView.findViewById(R.id.app_icon);
        TextView name = convertView.findViewById(R.id.app_name);

        icon.setImageDrawable(app.icon);
        name.setText(app.label);

        convertView.setOnClickListener(v -> {
            Intent launchIntent = context.getPackageManager()
                    .getLaunchIntentForPackage(app.packageName);
            if (launchIntent != null) {
                context.startActivity(launchIntent);
            }
        });

        convertView.setOnLongClickListener(v -> {
            showAppOptions(app);
            return true;
        });

        convertView.setOnTouchListener(new View.OnTouchListener() {
            private long startTime = 0;
            private boolean dragged = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startTime = System.currentTimeMillis();
                        dragged = false;
                        return false;

                    case MotionEvent.ACTION_MOVE:
                        if (!dragged && System.currentTimeMillis() - startTime > 300) {
                            View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                v.startDragAndDrop(null, shadow, app, 0);
                            }
                            dragged = true;
                        }

                        float y = event.getRawY();
                        if (y > v.getRootView().getHeight() - 100) {
                            Intent intent = new Intent(context, HomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra("dragging_app_package", app.packageName);
                            context.startActivity(intent);
                        }

                        return true;

                    case MotionEvent.ACTION_UP:
                        return false;
                }
                return false;
            }
        });

        return convertView;
    }

    public void filter(String keyword) {
        filteredApps.clear();
        if (keyword.isEmpty()) {
            filteredApps.addAll(originalApps);
        } else {
            for (AppInfo app : originalApps) {
                if (app.label.toLowerCase().contains(keyword)) {
                    filteredApps.add(app);
                }
            }
        }
        notifyDataSetChanged();
    }

    private void showAppOptions(AppInfo app) {
        String[] options = {"App Info", "Uninstall", "Add to Screen", "Cancel"};

        new AlertDialog.Builder(context)
                .setTitle(app.label)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Intent infoIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            infoIntent.setData(Uri.parse("package:" + app.packageName));
                            context.startActivity(infoIntent);
                            break;

                        case 1:
                            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE);
                            uninstallIntent.setData(Uri.parse("package:" + app.packageName));
                            context.startActivity(uninstallIntent);
                            break;

                        case 2:
                            Intent homeIntent = new Intent(context, HomeActivity.class);
                            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            homeIntent.putExtra("add_to_home_package", app.packageName);
                            context.startActivity(homeIntent);
                            break;

                        case 3:
                            dialog.dismiss();
                            break;
                    }
                }).show();
    }
}
