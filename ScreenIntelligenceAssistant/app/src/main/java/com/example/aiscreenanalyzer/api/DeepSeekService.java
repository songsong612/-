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

public class DeepSeekService {
    private static final String TAG = "DeepSeekService";
    private static final String BASE_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String MODEL = "deepseek-vision";
    
    private String apiKey;
    
    public void initialize(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public void analyzeImage(Bitmap image, String question, AIServiceManager.AnalysisCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onError("DeepSeek API key not set");
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
                requestBody.put("model", MODEL);
                
                JSONArray messages = new JSONArray();
                
                // Add system message
                JSONObject systemMessage = new JSONObject();
                systemMessage.put("role", "system");
                systemMessage.put("content", "You are a helpful assistant that analyzes images and provides detailed information.");
                messages.put(systemMessage);
                
                // Add user message with image and question
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                
                JSONArray contentArray = new JSONArray();
                
                // Add image content
                JSONObject imageContent = new JSONObject();
                imageContent.put("type", "image_url");
                
                JSONObject imageUrl = new JSONObject();
                imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
                imageUrl.put("detail", "high");
                
                imageContent.put("image_url", imageUrl);
                contentArray.put(imageContent);
                
                // Add text content
                JSONObject textContent = new JSONObject();
                textContent.put("type", "text");
                textContent.put("text", question);
                contentArray.put(textContent);
                
                userMessage.put("content", contentArray);
                messages.put(userMessage);
                
                requestBody.put("messages", messages);
                
                // Set additional parameters
                requestBody.put("temperature", 0.7);
                requestBody.put("max_tokens", 1000);
                
                // Send the request
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .build();
                
                RequestBody body = RequestBody.create(
                        MediaType.parse("application/json"), requestBody.toString());
                
                Request request = new Request.Builder()
                        .url(BASE_URL)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .post(body)
                        .build();
                
                Response response = client.newCall(request).execute();
                ResponseBody responseBody = response.body();
                
                if (!response.isSuccessful() || responseBody == null) {
                    String errorMsg = responseBody != null ? responseBody.string() : "Unknown error";
                    Log.e(TAG, "DeepSeek API error: " + errorMsg);
                    callback.onError("API Error: " + response.code() + " " + errorMsg);
                    return;
                }
                
                String responseString = responseBody.string();
                JSONObject jsonResponse = new JSONObject(responseString);
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    String content = message.getString("content");
                    callback.onSuccess(content);
                } else {
                    callback.onError("No response from DeepSeek API");
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