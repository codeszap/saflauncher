package com.example.saflauncher;

import android.content.Intent;
import android.graphics.drawable.Drawable;

public class ShortcutInfoModel {
    public String appLabel;
    public CharSequence shortLabel;
    public Drawable icon;
    public Intent intent;

    public ShortcutInfoModel(String appLabel, CharSequence shortLabel, Drawable icon, Intent intent) {
        this.appLabel = appLabel;
        this.shortLabel = shortLabel;
        this.icon = icon;
        this.intent = intent;
    }
}
