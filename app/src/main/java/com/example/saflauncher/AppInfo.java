package com.example.saflauncher;

import android.graphics.drawable.Drawable;

public class AppInfo {
    public String label;
    public Drawable icon;
    public String packageName;

    // 🟢 Default constructor
    public AppInfo() {}

    // Optional: Constructor with all fields
    public AppInfo(String label, Drawable icon, String packageName) {
        this.label = label;
        this.icon = icon;
        this.packageName = packageName;
    }
}
