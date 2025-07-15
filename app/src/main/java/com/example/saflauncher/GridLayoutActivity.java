package com.example.saflauncher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

public class GridLayoutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 80, 40, 40);

        SharedPreferences prefs = getSharedPreferences("saf_launcher_prefs", MODE_PRIVATE);

        // 🔢 Grid Column Count
        TextView gridLabel = new TextView(this);
        gridLabel.setText("Choose Icons per Row:");
        gridLabel.setTextSize(18);
        root.addView(gridLabel);

        Spinner gridSpinner = new Spinner(this);
        String[] gridOptions = {"3", "4", "5", "6", "7"};
        ArrayAdapter<String> gridAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, gridOptions);
        gridSpinner.setAdapter(gridAdapter);
        root.addView(gridSpinner);

        int savedGrid = prefs.getInt("grid_column_count", 5);
        gridSpinner.setSelection(getIndexFromValue(gridOptions, String.valueOf(savedGrid)));

        // 📏 Icon Size
        TextView sizeLabel = new TextView(this);
        sizeLabel.setText("\nChoose Icon Size (px):");
        sizeLabel.setTextSize(18);
        root.addView(sizeLabel);

        Spinner sizeSpinner = new Spinner(this);
        String[] sizeOptions = {"60", "80", "100", "120", "150"};
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sizeOptions);
        sizeSpinner.setAdapter(sizeAdapter);
        root.addView(sizeSpinner);

        int savedSize = prefs.getInt("grid_icon_size", 100);
        sizeSpinner.setSelection(getIndexFromValue(sizeOptions, String.valueOf(savedSize)));

        // 🟠 Icon Shape
        TextView shapeLabel = new TextView(this);
        shapeLabel.setText("\nChoose Icon Shape:");
        shapeLabel.setTextSize(18);
        root.addView(shapeLabel);

        Spinner shapeSpinner = new Spinner(this);
        String[] shapeOptions = {"Square", "Rounded", "Circle"};
        ArrayAdapter<String> shapeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, shapeOptions);
        shapeSpinner.setAdapter(shapeAdapter);
        root.addView(shapeSpinner);

        String savedShape = prefs.getString("grid_icon_shape", "Square");
        shapeSpinner.setSelection(getIndexFromValue(shapeOptions, savedShape));

        // ✅ Apply Button
        Button applyButton = new Button(this);
        applyButton.setText("Apply and Open App Drawer");
        root.addView(applyButton);

        applyButton.setOnClickListener(v -> {
            int columnCount = Integer.parseInt((String) gridSpinner.getSelectedItem());
            int iconSize = Integer.parseInt((String) sizeSpinner.getSelectedItem());
            String iconShape = (String) shapeSpinner.getSelectedItem();

            // 💾 Save values
            getSharedPreferences("saf_launcher_prefs", MODE_PRIVATE)
                    .edit()
                    .putInt("grid_column_count", columnCount)
                    .putInt("grid_icon_size", iconSize)
                    .putString("grid_icon_shape", iconShape)
                    .apply();

            startActivity(new Intent(this, AppDrawerActivity.class));
            finish(); // optional: avoid going back here
        });

        setContentView(root);
    }

    private int getIndexFromValue(String[] options, String value) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) return i;
        }
        return 0; // default
    }
}
