package com.example.saflauncher;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LauncherSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(40, 80, 40, 40);

        // Title
        TextView title = new TextView(this);
        title.setText("⚙️ Launcher Settings");
        title.setTextSize(22);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.START);
        title.setPadding(0, 0, 0, 40);
        root.addView(title);

        // "Gestures" option
        TextView gestures = new TextView(this);
        gestures.setText("➡️ Gestures");
        gestures.setTextSize(18);
        gestures.setTextColor(Color.LTGRAY);
        gestures.setPadding(0, 20, 0, 20);
        gestures.setOnClickListener(v -> {
            Intent intent = new Intent(this, GestureSettingsActivity.class);
            startActivity(intent);
        });
        root.addView(gestures);

        // ✅ "Alias" option
        TextView alias = new TextView(this);
        alias.setText("✏️ Set App Alias");
        alias.setTextSize(18);
        alias.setTextColor(Color.LTGRAY);
        alias.setPadding(0, 20, 0, 20);
        alias.setOnClickListener(v -> {
            Intent intent = new Intent(this, AliasSettingsActivity.class);
            startActivity(intent);
        });
        root.addView(alias);

        // ✅ "Grid Layout" option
        TextView gridLayout = new TextView(this);
        gridLayout.setText("️️✏ Grid Layout");
        gridLayout.setTextSize(18);
        gridLayout.setTextColor(Color.LTGRAY);
        gridLayout.setPadding(0, 20, 0, 20);
        gridLayout.setOnClickListener(v -> {
            Intent intent = new Intent(this, GridLayoutActivity.class);
            startActivity(intent);
        });
        root.addView(gridLayout);

        setContentView(root);
    }
}
