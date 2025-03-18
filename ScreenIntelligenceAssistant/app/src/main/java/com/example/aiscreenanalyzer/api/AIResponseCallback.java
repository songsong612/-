package com.example.aiscreenanalyzer.api;

/**
 * Interface for callbacks when AI services respond
 */
public interface AIResponseCallback {
    /**
     * Called when an AI service response is ready
     * 
     * @param response The response text from the AI service, or null if there was an error
     * @param error Error message if something went wrong, or null if successful
     */
    void onResponse(String response, String error);
}