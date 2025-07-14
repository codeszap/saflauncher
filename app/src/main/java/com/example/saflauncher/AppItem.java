package com.example.saflauncher;

import android.graphics.drawable.Drawable;

public class AppItem {
    String label;
    String packageName;
    Drawable icon;

    public AppItem(String label, String packageName, Drawable icon) {
        this.label = label;
        this.packageName = packageName;
        this.icon = icon;
    }
}
