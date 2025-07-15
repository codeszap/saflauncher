package com.example.saflauncher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.*;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {

    private final Context context;
    private final List<AppInfo> originalApps;
    private final List<AppInfo> filteredApps;
    private final int iconSize;
    private final String iconShape;

    public AppListAdapter(Context context, List<AppInfo> apps, int iconSize) {
        this.context = context;
        this.originalApps = new ArrayList<>(apps);
        this.filteredApps = new ArrayList<>(apps);
        this.iconSize = iconSize;

        SharedPreferences prefs = context.getSharedPreferences("saf_launcher_prefs", Context.MODE_PRIVATE);
        this.iconShape = prefs.getString("icon_shape", "Square");
    }

    public List<AppInfo> getFilteredList() {
        return filteredApps;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app_icon, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = filteredApps.get(position);

        holder.iconView.setImageDrawable(app.icon);
        holder.labelView.setText(app.label);

        // 🖼️ Set icon size
        ViewGroup.LayoutParams params = holder.iconView.getLayoutParams();
        params.width = iconSize;
        params.height = iconSize;
        holder.iconView.setLayoutParams(params);

        // 🟠 Apply icon shape
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            switch (iconShape) {
                case "Rounded":
                    holder.iconView.setBackgroundResource(R.drawable.rounded);
                    holder.iconView.setClipToOutline(true);
                    break;
                case "Circle":
                    holder.iconView.setBackgroundResource(R.drawable.circle);
                    holder.iconView.setClipToOutline(true);
                    break;
                default:
                    holder.iconView.setBackground(null);
                    holder.iconView.setClipToOutline(false);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            Intent launchIntent = context.getPackageManager()
                    .getLaunchIntentForPackage(app.packageName);
            if (launchIntent != null) {
                context.startActivity(launchIntent);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            showAppOptions(app);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return filteredApps.size();
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconView;
        TextView labelView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.app_icon);
            labelView = itemView.findViewById(R.id.app_name);
        }
    }
}
