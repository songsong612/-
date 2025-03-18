package com.example.aiscreenanalyzer;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {

    private SeekBar opacitySeekBar;
    private TextView opacityValueText;
    private SeekBar sizeSeekBar;
    private TextView sizeValueText;
    private EditText deepseekApiKeyEdit;
    private EditText googleApiKeyEdit;
    private EditText grokApiKeyEdit;
    private Button saveButton;
    private Button cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Enable back button in action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.settings_title);
        }

        // Initialize views
        opacitySeekBar = findViewById(R.id.seekbar_opacity);
        opacityValueText = findViewById(R.id.text_opacity_value);
        sizeSeekBar = findViewById(R.id.seekbar_size);
        sizeValueText = findViewById(R.id.text_size_value);
        deepseekApiKeyEdit = findViewById(R.id.edit_deepseek_api_key);
        googleApiKeyEdit = findViewById(R.id.edit_google_api_key);
        grokApiKeyEdit = findViewById(R.id.edit_grok_api_key);
        saveButton = findViewById(R.id.btn_save);
        cancelButton = findViewById(R.id.btn_cancel);

        // Load saved settings
        loadSettings();

        // Set up listeners
        setupListeners();
    }

    private void loadSettings() {
        // Get saved settings from preferences
        var prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Load opacity setting (default 70%)
        int opacity = prefs.getInt("window_opacity", 70);
        opacitySeekBar.setProgress(opacity);
        opacityValueText.setText(opacity + "%");
        
        // Load size setting (default 100%)
        int size = prefs.getInt("window_size", 100);
        sizeSeekBar.setProgress(size);
        sizeValueText.setText(size + "%");
        
        // Load API keys
        String deepseekApiKey = prefs.getString("deepseek_api_key", "");
        String googleApiKey = prefs.getString("google_api_key", "");
        String grokApiKey = prefs.getString("grok_api_key", "");
        
        deepseekApiKeyEdit.setText(deepseekApiKey);
        googleApiKeyEdit.setText(googleApiKey);
        grokApiKeyEdit.setText(grokApiKey);
    }

    private void setupListeners() {
        // Opacity seek bar listener
        opacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                opacityValueText.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not needed
            }
        });

        // Size seek bar listener
        sizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sizeValueText.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not needed
            }
        });

        // Save button listener
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
                Toast.makeText(SettingsActivity.this, "Settings saved", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        // Cancel button listener
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void saveSettings() {
        // Save settings to preferences
        var prefs = PreferenceManager.getDefaultSharedPreferences(this);
        var editor = prefs.edit();
        
        // Save opacity and size
        editor.putInt("window_opacity", opacitySeekBar.getProgress());
        editor.putInt("window_size", sizeSeekBar.getProgress());
        
        // Save API keys
        editor.putString("deepseek_api_key", deepseekApiKeyEdit.getText().toString().trim());
        editor.putString("google_api_key", googleApiKeyEdit.getText().toString().trim());
        editor.putString("grok_api_key", grokApiKeyEdit.getText().toString().trim());
        
        editor.apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}