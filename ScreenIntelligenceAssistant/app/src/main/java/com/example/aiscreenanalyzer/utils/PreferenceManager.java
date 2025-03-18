package com.example.aiscreenanalyzer.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String PREFS_NAME = "AIScreenAnalyzerPrefs";
    private static final String KEY_PREFIX = "api_key_";
    
    private SharedPreferences sharedPreferences;
    
    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public void saveApiKey(String serviceType, String apiKey) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_PREFIX + serviceType, apiKey);
        editor.apply();
    }
    
    public String getApiKey(String serviceType) {
        return sharedPreferences.getString(KEY_PREFIX + serviceType, "");
    }
    
    public void clearApiKey(String serviceType) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_PREFIX + serviceType);
        editor.apply();
    }
    
    public void clearAllApiKeys() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}
