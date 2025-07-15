package com.example.saflauncher;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import java.util.*;

public class AliasSettingsActivity extends AppCompatActivity {

    private LinearLayout aliasListLayout;
    private SharedPreferences prefs;
    private Map<String, String> aliasMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 80, 40, 40);
        scrollView.addView(root);

        prefs = getSharedPreferences("alias_map", MODE_PRIVATE);
        aliasMap = new HashMap<>((Map<String, String>) prefs.getAll());

        TextView title = new TextView(this);
        title.setText("🔁 Alias Manager");
        title.setTextSize(22);
        root.addView(title);

        aliasListLayout = new LinearLayout(this);
        aliasListLayout.setOrientation(LinearLayout.VERTICAL);
        root.addView(aliasListLayout);

        renderAliasList();

        Button addAliasBtn = new Button(this);
        addAliasBtn.setText("➕ Create New Alias");
        addAliasBtn.setOnClickListener(v -> showAliasInputDialog(null, null));
        root.addView(addAliasBtn);

        setContentView(scrollView);
    }

    private void renderAliasList() {
        aliasListLayout.removeAllViews();

        for (String alias : aliasMap.keySet()) {
            String target = aliasMap.get(alias);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 30, 0, 30);

            TextView aliasView = new TextView(this);
            aliasView.setText("🔤 " + alias + " → " + target);
            aliasView.setTextSize(16);

            LinearLayout actionRow = new LinearLayout(this);
            actionRow.setOrientation(LinearLayout.HORIZONTAL);

            Button editBtn = new Button(this);
            editBtn.setText("✏️ Edit");
            editBtn.setOnClickListener(v -> showAliasInputDialog(alias, target));

            Button deleteBtn = new Button(this);
            deleteBtn.setText("❌ Delete");
            deleteBtn.setOnClickListener(v -> {
                prefs.edit().remove(alias).apply();
                aliasMap.remove(alias);
                renderAliasList();
            });

            actionRow.addView(editBtn);
            actionRow.addView(deleteBtn);

            row.addView(aliasView);
            row.addView(actionRow);
            aliasListLayout.addView(row);
        }

        if (aliasMap.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No aliases found. Add one! 😊");
            aliasListLayout.addView(emptyText);
        }
    }

    private void showAliasInputDialog(String existingAlias, String existingTarget) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(existingAlias == null ? "Create Alias" : "Edit Alias");

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(30, 30, 30, 30);

        EditText aliasInput = new EditText(this);
        aliasInput.setHint("Alias Name (e.g., yt)");
        if (existingAlias != null) aliasInput.setText(existingAlias);
        dialogLayout.addView(aliasInput);

        EditText targetInput = new EditText(this);
        targetInput.setHint("Target (package:com.app OR https://...)");
        if (existingTarget != null) targetInput.setText(existingTarget);
        dialogLayout.addView(targetInput);

        builder.setView(dialogLayout);

        builder.setPositiveButton("✅ Save", (dialog, which) -> {
            String alias = aliasInput.getText().toString().trim();
            String target = targetInput.getText().toString().trim();

            if (alias.isEmpty() || target.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit().putString(alias, target).apply();
            aliasMap.put(alias, target);
            renderAliasList();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
