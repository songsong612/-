package com.example.aiscreenanalyzer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.example.aiscreenanalyzer.api.AIServiceManager;
import com.example.aiscreenanalyzer.utils.BitmapUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class FloatingWindowService extends Service {
    private static final String TAG = "FloatingWindowService";
    private static final String NOTIFICATION_CHANNEL_ID = "floating_window_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR;
    private static final String SCREEN_CAPTURE_NAME = "ai_screen_capture";
    
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private static AtomicBoolean isRunning = new AtomicBoolean(false);
    
    // UI elements
    private LinearLayout collapsedView;
    private LinearLayout expandedView;
    private ImageView captureImageView;
    private EditText queryEditText;
    private TextView analysisResultText;
    private ProgressBar progressBar;
    private Spinner aiServiceSpinner;
    private Button captureButton;
    private Button analyzeButton;
    
    // Media projection for screen capture
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private int screenWidth;
    private int screenHeight;
    private int screenDensity;
    
    // Current capture bitmap
    private Bitmap currentCapture;
    
    // Current AI service
    private int currentAiService = AIServiceManager.AI_SERVICE_DEEPSEEK;
    
    // Handler for UI thread operations
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // Touch listener for moving the floating window
    private final View.OnTouchListener moveWindowTouchListener = new View.OnTouchListener() {
        private int initialX;
        private int initialY;
        private float initialTouchX;
        private float initialTouchY;
        
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;
                
                case MotionEvent.ACTION_MOVE:
                    params.x = initialX + (int) (event.getRawX() - initialTouchX);
                    params.y = initialY + (int) (event.getRawY() - initialTouchY);
                    windowManager.updateViewLayout(floatingView, params);
                    return true;
                
                default:
                    return false;
            }
        }
    };
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        isRunning.set(true);
        
        // Initialize window manager and layout parameters
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        initLayoutParams();
        
        // Get screen metrics
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;
        
        // Inflate floating window layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        floatingView = inflater.inflate(R.layout.layout_floating_window, null);
        
        // Add the view to window manager
        windowManager.addView(floatingView, params);
        
        // Initialize UI elements
        initializeViews();
        
        // Set up listeners
        setupListeners();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.notification_channel_description));
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        
        // Create notification
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        
        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(R.drawable.app_logo)
                .setContentIntent(pendingIntent)
                .build();
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, notification);
        
        // Initialize MediaProjection
        if (intent != null) {
            int resultCode = intent.getIntExtra("resultCode", -1);
            Intent resultData = intent.getParcelableExtra("resultData");
            
            if (resultCode != -1 && resultData != null) {
                MediaProjectionManager projectionManager = 
                        (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                mediaProjection = projectionManager.getMediaProjection(resultCode, resultData);
            }
        }
        
        // Load saved AI service preference
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String defaultService = prefs.getString("default_ai_service", 
                getString(R.string.ai_service_deepseek));
        
        if (defaultService.equals(getString(R.string.ai_service_deepseek))) {
            currentAiService = AIServiceManager.AI_SERVICE_DEEPSEEK;
        } else if (defaultService.equals(getString(R.string.ai_service_google))) {
            currentAiService = AIServiceManager.AI_SERVICE_GOOGLE;
        } else if (defaultService.equals(getString(R.string.ai_service_grok))) {
            currentAiService = AIServiceManager.AI_SERVICE_GROK;
        }
        
        if (aiServiceSpinner != null) {
            aiServiceSpinner.setSelection(currentAiService);
        }
        
        return START_REDELIVER_INTENT;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning.set(false);
        
        // Release media projection and virtual display
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        
        // Clean up floating window
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
    }
    
    /**
     * Initialize window layout parameters
     */
    private void initLayoutParams() {
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;
    }
    
    /**
     * Initialize UI views
     */
    private void initializeViews() {
        collapsedView = floatingView.findViewById(R.id.collapsed_view);
        expandedView = floatingView.findViewById(R.id.expanded_view);
        captureImageView = floatingView.findViewById(R.id.img_capture);
        queryEditText = floatingView.findViewById(R.id.edit_query);
        analysisResultText = floatingView.findViewById(R.id.txt_analysis_result);
        progressBar = floatingView.findViewById(R.id.progress_bar);
        aiServiceSpinner = floatingView.findViewById(R.id.spinner_ai_service);
        captureButton = floatingView.findViewById(R.id.btn_capture);
        analyzeButton = floatingView.findViewById(R.id.btn_analyze);
        
        // Setup AI service spinner
        String[] aiServices = {
                getString(R.string.ai_service_deepseek),
                getString(R.string.ai_service_google),
                getString(R.string.ai_service_grok)
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, aiServices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        aiServiceSpinner.setAdapter(adapter);
        aiServiceSpinner.setSelection(currentAiService);
    }
    
    /**
     * Set up event listeners for UI elements
     */
    private void setupListeners() {
        // Set touch listener for moving the floating window
        collapsedView.setOnTouchListener(moveWindowTouchListener);
        
        // Expand button click listener
        floatingView.findViewById(R.id.btn_expand).setOnClickListener(v -> {
            collapsedView.setVisibility(View.GONE);
            expandedView.setVisibility(View.VISIBLE);
        });
        
        // Collapse button click listener
        floatingView.findViewById(R.id.btn_collapse).setOnClickListener(v -> {
            expandedView.setVisibility(View.GONE);
            collapsedView.setVisibility(View.VISIBLE);
        });
        
        // Close button click listener
        floatingView.findViewById(R.id.btn_close).setOnClickListener(v -> {
            stopSelf();
        });
        
        // AI service selection listener
        aiServiceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentAiService = position;
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
        
        // Capture button click listener
        captureButton.setOnClickListener(v -> captureScreen());
        
        // Analyze button click listener
        analyzeButton.setOnClickListener(v -> {
            if (currentCapture != null) {
                String query = queryEditText.getText().toString().trim();
                analyzeImage(currentCapture, query);
            } else {
                showToast(getString(R.string.error_capture));
            }
        });
    }
    
    /**
     * Capture the current screen
     */
    private void captureScreen() {
        if (mediaProjection == null) {
            showToast("Media projection not initialized");
            return;
        }
        
        // Show progress indicator
        progressBar.setVisibility(View.VISIBLE);
        
        // Set up image reader
        setupImageReader();
        
        // Create virtual display
        virtualDisplay = mediaProjection.createVirtualDisplay(
                SCREEN_CAPTURE_NAME,
                screenWidth,
                screenHeight,
                screenDensity,
                VIRTUAL_DISPLAY_FLAGS,
                imageReader.getSurface(),
                null,
                null);
        
        // Wait for the image to be ready
        mainHandler.postDelayed(this::processScreenCapture, 200);
    }
    
    /**
     * Setup the image reader for screen capture
     */
    private void setupImageReader() {
        if (imageReader != null) {
            imageReader.close();
        }
        
        imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2);
    }
    
    /**
     * Process the captured screen image
     */
    private void processScreenCapture() {
        Image image = null;
        try {
            image = imageReader.acquireLatestImage();
            
            if (image != null) {
                // Get image data
                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * screenWidth;
                
                // Create bitmap from image data
                Bitmap bitmap = Bitmap.createBitmap(
                        screenWidth + rowPadding / pixelStride,
                        screenHeight,
                        Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(buffer);
                
                // Crop to actual screen size
                Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight);
                bitmap.recycle();
                
                // Store the captured image
                currentCapture = croppedBitmap;
                
                // Display the captured image
                mainHandler.post(() -> {
                    captureImageView.setImageBitmap(currentCapture);
                    progressBar.setVisibility(View.GONE);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error capturing screen", e);
            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                showToast(getString(R.string.error_capture));
            });
        } finally {
            if (image != null) {
                image.close();
            }
            
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
        }
    }
    
    /**
     * Analyze the captured image using the selected AI service
     */
    private void analyzeImage(Bitmap bitmap, String query) {
        // Show progress indicator
        progressBar.setVisibility(View.VISIBLE);
        
        // Clear previous results
        analysisResultText.setText("");
        
        // Scale down bitmap if it's too large
        Bitmap scaledBitmap = BitmapUtils.scaleBitmap(bitmap, 1024, 1024);
        
        // Call AI service to analyze the image
        AIServiceManager.getInstance(this).analyzeImage(
                scaledBitmap,
                query,
                currentAiService,
                new AIServiceManager.AnalysisCallback() {
                    @Override
                    public void onSuccess(String result) {
                        mainHandler.post(() -> {
                            analysisResultText.setText(result);
                            progressBar.setVisibility(View.GONE);
                        });
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        mainHandler.post(() -> {
                            analysisResultText.setText(getString(R.string.analysis_error) + "\n" + errorMessage);
                            progressBar.setVisibility(View.GONE);
                        });
                    }
                });
    }
    
    /**
     * Show a toast message on the UI thread
     */
    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
    
    /**
     * Check if the service is running
     */
    public static boolean isRunning() {
        return isRunning.get();
    }
}