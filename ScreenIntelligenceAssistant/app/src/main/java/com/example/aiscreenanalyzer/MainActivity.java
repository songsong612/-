package com.example.aiscreenanalyzer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY_PERMISSION = 101;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 102;
    
    private Button startServiceButton;
    private Button permissionButton;
    private TextView serviceStatusText;
    private TextView permissionsStatusText;
    private Spinner defaultAiSpinner;
    private MediaProjectionManager mediaProjectionManager;
    private Intent resultData = null;
    private boolean isServiceRunning = false;
    private SharedPreferences sharedPreferences;
    
    private ActivityResultLauncher<Intent> screenCaptureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    resultData = result.getData();
                    startFloatingWindowService();
                } else {
                    Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Initialize views
        startServiceButton = findViewById(R.id.btn_start_service);
        permissionButton = findViewById(R.id.btn_permission);
        serviceStatusText = findViewById(R.id.service_status_text);
        permissionsStatusText = findViewById(R.id.permissions_status_text);
        defaultAiSpinner = findViewById(R.id.spinner_default_ai);
        
        // Setup AI service spinner
        setupAiServiceSpinner();
        
        // Get MediaProjectionManager service
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        
        // Set button click listeners
        setupButtonListeners();
        
        // Check permissions
        updatePermissionStatus();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Check if service is running
        isServiceRunning = FloatingWindowService.isRunning();
        updateServiceStatus();
        
        // Check permissions (overlay permission may have changed)
        updatePermissionStatus();
    }
    
    private void setupAiServiceSpinner() {
        String[] aiServices = {
                getString(R.string.ai_service_deepseek),
                getString(R.string.ai_service_google),
                getString(R.string.ai_service_grok)
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, aiServices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        defaultAiSpinner.setAdapter(adapter);
        
        // Set default selection based on saved preference
        String defaultService = sharedPreferences.getString("default_ai_service", 
                getString(R.string.ai_service_deepseek));
        for (int i = 0; i < aiServices.length; i++) {
            if (aiServices[i].equals(defaultService)) {
                defaultAiSpinner.setSelection(i);
                break;
            }
        }
    }
    
    private void setupButtonListeners() {
        startServiceButton.setOnClickListener(v -> {
            if (!isServiceRunning) {
                // Save default AI service selection
                String selectedService = (String) defaultAiSpinner.getSelectedItem();
                sharedPreferences.edit().putString("default_ai_service", selectedService).apply();
                
                // Start screen capture
                if (hasAllPermissions()) {
                    Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
                    screenCaptureLauncher.launch(captureIntent);
                } else {
                    Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show();
                }
            } else {
                // Stop service
                stopService(new Intent(MainActivity.this, FloatingWindowService.class));
                isServiceRunning = false;
                updateServiceStatus();
            }
        });
        
        permissionButton.setOnClickListener(v -> requestMissingPermissions());
        
        findViewById(R.id.btn_settings).setOnClickListener(v -> {
            Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
        });
    }
    
    private void startFloatingWindowService() {
        if (resultData != null) {
            Intent intent = new Intent(this, FloatingWindowService.class);
            intent.putExtra("resultCode", RESULT_OK);
            intent.putExtra("resultData", resultData);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            
            isServiceRunning = true;
            updateServiceStatus();
        }
    }
    
    private void updateServiceStatus() {
        if (isServiceRunning) {
            serviceStatusText.setText(R.string.service_running);
            startServiceButton.setText(R.string.stop_service);
        } else {
            serviceStatusText.setText(R.string.service_stopped);
            startServiceButton.setText(R.string.start_service);
        }
    }
    
    private boolean hasAllPermissions() {
        boolean hasOverlayPermission = Settings.canDrawOverlays(this);
        boolean hasNotificationPermission = true;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(this, 
                    android.Manifest.permission.POST_NOTIFICATIONS) == 
                    android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        
        return hasOverlayPermission && hasNotificationPermission;
    }
    
    private void updatePermissionStatus() {
        boolean hasAllPerms = hasAllPermissions();
        permissionsStatusText.setText(hasAllPerms ? 
                "All permissions granted" : getString(R.string.permission_required));
        permissionButton.setVisibility(hasAllPerms ? View.GONE : View.VISIBLE);
        startServiceButton.setEnabled(hasAllPerms);
    }
    
    private void requestMissingPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, 
                    android.Manifest.permission.POST_NOTIFICATIONS) != 
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            updatePermissionStatus();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            updatePermissionStatus();
        }
    }
}