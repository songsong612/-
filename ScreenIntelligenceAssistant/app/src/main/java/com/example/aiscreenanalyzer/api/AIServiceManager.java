package com.example.aiscreenanalyzer.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.aiscreenanalyzer.BuildConfig;
import com.example.aiscreenanalyzer.R;
import com.example.aiscreenanalyzer.utils.BitmapUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIServiceManager {
    private static final String TAG = "AIServiceManager";
    
    // AI Service types
    public static final int AI_SERVICE_DEEPSEEK = 0;
    public static final int AI_SERVICE_GOOGLE = 1;
    public static final int AI_SERVICE_GROK = 2;
    
    // Cache for recent analyses to reduce API calls
    private static final Map<String, String> analysisCache = new HashMap<>();
    
    // Endpoint URLs
    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String GOOGLE_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro-vision:generateContent";
    private static final String GROK_API_URL = "https://api.grok.x/v1/chat/completions";
    
    // Singleton instance
    private static AIServiceManager instance;
    
    private final Context context;
    private final OkHttpClient client;
    
    // Private constructor for singleton
    private AIServiceManager(Context context) {
        this.context = context.getApplicationContext();
        
        // Configure OkHttpClient with timeouts
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    // Get singleton instance
    public static synchronized AIServiceManager getInstance(Context context) {
        if (instance == null) {
            instance = new AIServiceManager(context);
        }
        return instance;
    }
    
    /**
     * Analyzes a bitmap image using the selected AI service
     */
    public void analyzeImage(Bitmap bitmap, String query, int serviceType, AnalysisCallback callback) {
        if (bitmap == null) {
            callback.onError("Bitmap is null");
            return;
        }
        
        // Create cache key from bitmap hash and query
        String cacheKey = BitmapUtils.getBitmapHash(bitmap) + "_" + query + "_" + serviceType;
        
        // Check cache first
        if (analysisCache.containsKey(cacheKey)) {
            String cachedResult = analysisCache.get(cacheKey);
            callback.onSuccess(cachedResult);
            return;
        }
        
        // Convert bitmap to byte array (JPEG format with 80% quality for better performance)
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        
        try {
            outputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing output stream", e);
        }
        
        switch (serviceType) {
            case AI_SERVICE_DEEPSEEK:
                analyzeWithDeepSeek(imageBytes, query, cacheKey, callback);
                break;
            case AI_SERVICE_GOOGLE:
                analyzeWithGoogleAI(imageBytes, query, cacheKey, callback);
                break;
            case AI_SERVICE_GROK:
                analyzeWithGrok(imageBytes, query, cacheKey, callback);
                break;
            default:
                callback.onError("Invalid AI service type");
        }
    }
    
    /**
     * Analyzes image using DeepSeek API
     */
    private void analyzeWithDeepSeek(byte[] imageBytes, String query, String cacheKey, AnalysisCallback callback) {
        String apiKey = BuildConfig.DEEPSEEK_API_KEY;
        if (apiKey.equals("YOUR_DEEPSEEK_API_KEY")) {
            callback.onError(context.getString(R.string.error_api_key));
            return;
        }
        
        String base64Image = BitmapUtils.bytesToBase64(imageBytes);
        
        try {
            // Create JSON payload
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "deepseek-vision");
            
            JSONArray messagesArray = new JSONArray();
            
            // User message with image
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            
            // Content for user message
            JSONArray contentArray = new JSONArray();
            
            // Text content
            JSONObject textContent = new JSONObject();
            textContent.put("type", "text");
            
            // If query is empty, use default prompt
            String finalQuery = query.isEmpty() ? 
                    "What's in this image? Provide a detailed description." : query;
            textContent.put("text", finalQuery);
            contentArray.put(textContent);
            
            // Image content
            JSONObject imageContent = new JSONObject();
            imageContent.put("type", "image_url");
            
            JSONObject imageUrl = new JSONObject();
            imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
            
            imageContent.put("image_url", imageUrl);
            contentArray.put(imageContent);
            
            userMessage.put("content", contentArray);
            messagesArray.put(userMessage);
            
            jsonBody.put("messages", messagesArray);
            
            // Create request
            Request request = new Request.Builder()
                    .url(DEEPSEEK_API_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(RequestBody.create(jsonBody.toString(), 
                            MediaType.parse("application/json")))
                    .build();
            
            // Execute the request asynchronously
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onError("API error: " + response.code() + " " + response.message());
                        return;
                    }
                    
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray choices = jsonResponse.getJSONArray("choices");
                        JSONObject firstChoice = choices.getJSONObject(0);
                        JSONObject message = firstChoice.getJSONObject("message");
                        String content = message.getString("content");
                        
                        // Cache result
                        analysisCache.put(cacheKey, content);
                        
                        // Return result
                        callback.onSuccess(content);
                    } catch (JSONException e) {
                        callback.onError("Error parsing response: " + e.getMessage());
                    }
                }
            });
        } catch (JSONException e) {
            callback.onError("Error creating request: " + e.getMessage());
        }
    }
    
    /**
     * Analyzes image using Google AI (Gemini Pro Vision)
     */
    private void analyzeWithGoogleAI(byte[] imageBytes, String query, String cacheKey, AnalysisCallback callback) {
        String apiKey = BuildConfig.GOOGLE_API_KEY;
        if (apiKey.equals("YOUR_GOOGLE_API_KEY")) {
            callback.onError(context.getString(R.string.error_api_key));
            return;
        }
        
        String base64Image = BitmapUtils.bytesToBase64(imageBytes);
        
        try {
            // Create JSON payload
            JSONObject jsonBody = new JSONObject();
            
            // If query is empty, use default prompt
            String finalQuery = query.isEmpty() ? 
                    "What's in this image? Provide a detailed description." : query;
            
            // Add content parts
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            
            // Add text part
            JSONObject textPart = new JSONObject();
            textPart.put("text", finalQuery);
            parts.put(textPart);
            
            // Add image part
            JSONObject imagePart = new JSONObject();
            JSONObject inlineData = new JSONObject();
            inlineData.put("mimeType", "image/jpeg");
            inlineData.put("data", base64Image);
            imagePart.put("inlineData", inlineData);
            parts.put(imagePart);
            
            content.put("parts", parts);
            contents.put(content);
            jsonBody.put("contents", contents);
            
            // Create request
            String url = GOOGLE_API_URL + "?key=" + apiKey;
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody.toString(), 
                            MediaType.parse("application/json")))
                    .build();
            
            // Execute the request asynchronously
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onError("API error: " + response.code() + " " + response.message());
                        return;
                    }
                    
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray candidates = jsonResponse.getJSONArray("candidates");
                        JSONObject firstCandidate = candidates.getJSONObject(0);
                        JSONObject candidateContent = firstCandidate.getJSONObject("content");
                        JSONArray responseParts = candidateContent.getJSONArray("parts");
                        JSONObject firstPart = responseParts.getJSONObject(0);
                        String text = firstPart.getString("text");
                        
                        // Cache result
                        analysisCache.put(cacheKey, text);
                        
                        // Return result
                        callback.onSuccess(text);
                    } catch (JSONException e) {
                        callback.onError("Error parsing response: " + e.getMessage());
                    }
                }
            });
        } catch (JSONException e) {
            callback.onError("Error creating request: " + e.getMessage());
        }
    }
    
    /**
     * Analyzes image using Grok API
     */
    private void analyzeWithGrok(byte[] imageBytes, String query, String cacheKey, AnalysisCallback callback) {
        String apiKey = BuildConfig.GROK_API_KEY;
        if (apiKey.equals("YOUR_GROK_API_KEY")) {
            callback.onError(context.getString(R.string.error_api_key));
            return;
        }
        
        String base64Image = BitmapUtils.bytesToBase64(imageBytes);
        
        try {
            // Create JSON payload
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "grok-vision");
            
            JSONArray messagesArray = new JSONArray();
            
            // User message with image
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            
            // Content for user message
            JSONArray contentArray = new JSONArray();
            
            // Text content
            JSONObject textContent = new JSONObject();
            textContent.put("type", "text");
            
            // If query is empty, use default prompt
            String finalQuery = query.isEmpty() ? 
                    "What's in this image? Provide a detailed description." : query;
            textContent.put("text", finalQuery);
            contentArray.put(textContent);
            
            // Image content
            JSONObject imageContent = new JSONObject();
            imageContent.put("type", "image");
            imageContent.put("image_data", base64Image);
            contentArray.put(imageContent);
            
            userMessage.put("content", contentArray);
            messagesArray.put(userMessage);
            
            jsonBody.put("messages", messagesArray);
            
            // Create request
            Request request = new Request.Builder()
                    .url(GROK_API_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(RequestBody.create(jsonBody.toString(), 
                            MediaType.parse("application/json")))
                    .build();
            
            // Execute the request asynchronously
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onError("API error: " + response.code() + " " + response.message());
                        return;
                    }
                    
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray choices = jsonResponse.getJSONArray("choices");
                        JSONObject firstChoice = choices.getJSONObject(0);
                        JSONObject message = firstChoice.getJSONObject("message");
                        String content = message.getString("content");
                        
                        // Cache result
                        analysisCache.put(cacheKey, content);
                        
                        // Return result
                        callback.onSuccess(content);
                    } catch (JSONException e) {
                        callback.onError("Error parsing response: " + e.getMessage());
                    }
                }
            });
        } catch (JSONException e) {
            callback.onError("Error creating request: " + e.getMessage());
        }
    }
    
    /**
     * Clears the analysis cache
     */
    public void clearCache() {
        analysisCache.clear();
    }
    
    /**
     * Callback interface for image analysis
     */
    public interface AnalysisCallback {
        void onSuccess(String result);
        void onError(String errorMessage);
    }
}