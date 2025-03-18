package com.example.aiscreenanalyzer.api;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GoogleAIService {
    private static final String TAG = "GoogleAIService";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent";
    private static final String API_VERSION = "v1";
    
    private String apiKey;
    
    public void initialize(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public void analyzeImage(Bitmap image, String question, AIServiceManager.AnalysisCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onError("Google AI API key not set");
            return;
        }
        
        new Thread(() -> {
            try {
                // Encode the image to base64
                String base64Image = bitmapToBase64(image);
                if (base64Image == null) {
                    callback.onError("Failed to encode image");
                    return;
                }
                
                // Create the JSON request body
                JSONObject requestBody = new JSONObject();
                
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                
                JSONArray parts = new JSONArray();
                
                // Add text part
                JSONObject textPart = new JSONObject();
                textPart.put("text", question);
                parts.put(textPart);
                
                // Add image part
                JSONObject imagePart = new JSONObject();
                imagePart.put("inline_data", new JSONObject()
                        .put("mime_type", "image/jpeg")
                        .put("data", base64Image));
                parts.put(imagePart);
                
                content.put("parts", parts);
                contents.put(content);
                
                requestBody.put("contents", contents);
                
                // Add generation config
                JSONObject generationConfig = new JSONObject();
                generationConfig.put("temperature", 0.7);
                generationConfig.put("maxOutputTokens", 1000);
                requestBody.put("generationConfig", generationConfig);
                
                // Send the request
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .build();
                
                RequestBody body = RequestBody.create(
                        MediaType.parse("application/json"), requestBody.toString());
                
                String url = BASE_URL + "?key=" + apiKey;
                
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();
                
                Response response = client.newCall(request).execute();
                ResponseBody responseBody = response.body();
                
                if (!response.isSuccessful() || responseBody == null) {
                    String errorMsg = responseBody != null ? responseBody.string() : "Unknown error";
                    Log.e(TAG, "Google AI API error: " + errorMsg);
                    callback.onError("API Error: " + response.code() + " " + errorMsg);
                    return;
                }
                
                String responseString = responseBody.string();
                JSONObject jsonResponse = new JSONObject(responseString);
                
                // Parse the Gemini response format
                if (jsonResponse.has("candidates")) {
                    JSONArray candidates = jsonResponse.getJSONArray("candidates");
                    if (candidates.length() > 0) {
                        JSONObject candidate = candidates.getJSONObject(0);
                        JSONObject candidateContent = candidate.getJSONObject("content");
                        
                        if (candidateContent.has("parts")) {
                            JSONArray responseParts = candidateContent.getJSONArray("parts");
                            if (responseParts.length() > 0) {
                                JSONObject part = responseParts.getJSONObject(0);
                                if (part.has("text")) {
                                    String text = part.getString("text");
                                    callback.onSuccess(text);
                                } else {
                                    callback.onError("No text in Google AI response");
                                }
                            } else {
                                callback.onError("Empty parts array in Google AI response");
                            }
                        } else {
                            callback.onError("No parts in Google AI response");
                        }
                    } else {
                        callback.onError("No candidates in Google AI response");
                    }
                } else {
                    // Check if there's an error
                    if (jsonResponse.has("error")) {
                        JSONObject error = jsonResponse.getJSONObject("error");
                        String message = error.has("message") ? error.getString("message") : "Unknown error";
                        callback.onError("API Error: " + message);
                    } else {
                        callback.onError("Invalid response from Google AI");
                    }
                }
                
            } catch (JSONException e) {
                Log.e(TAG, "JSON error: " + e.getMessage(), e);
                callback.onError("JSON Error: " + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "Network error: " + e.getMessage(), e);
                callback.onError("Network Error: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error: " + e.getMessage(), e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }
    
    private String bitmapToBase64(Bitmap bitmap) {
        try {
            // Resize image to reduce API payload
            Bitmap resizedBitmap = resizeImageIfNeeded(bitmap);
            
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Error encoding bitmap: " + e.getMessage(), e);
            return null;
        }
    }
    
    private Bitmap resizeImageIfNeeded(Bitmap original) {
        int maxDimension = 1024; // Max dimension size
        
        int width = original.getWidth();
        int height = original.getHeight();
        
        float bitmapRatio = (float) width / (float) height;
        
        if (width > height && width > maxDimension) {
            width = maxDimension;
            height = (int) (width / bitmapRatio);
        } else if (height > width && height > maxDimension) {
            height = maxDimension;
            width = (int) (height * bitmapRatio);
        } else if (width > maxDimension && height > maxDimension) {
            if (bitmapRatio > 1) {
                width = maxDimension;
                height = (int) (width / bitmapRatio);
            } else {
                height = maxDimension;
                width = (int) (height * bitmapRatio);
            }
        } else {
            // No resize needed
            return original;
        }
        
        return Bitmap.createScaledBitmap(original, width, height, true);
    }
}