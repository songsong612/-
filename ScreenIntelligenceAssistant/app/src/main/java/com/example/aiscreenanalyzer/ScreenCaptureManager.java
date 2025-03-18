package com.example.aiscreenanalyzer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Manages screen capture functionality
 */
public class ScreenCaptureManager {
    
    private static final String TAG = "ScreenCaptureManager";
    
    private Context context;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private int width;
    private int height;
    private int density;
    
    /**
     * Constructor
     * 
     * @param context The application context
     * @param mediaProjection MediaProjection object for screen capture
     * @param width Screen width
     * @param height Screen height
     * @param density Screen density
     */
    public ScreenCaptureManager(Context context, MediaProjection mediaProjection, 
                                int width, int height, int density) {
        this.context = context;
        this.mediaProjection = mediaProjection;
        this.width = width;
        this.height = height;
        this.density = density;
        
        // Initialize the image reader
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        
        // Create virtual display
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.getSurface(),
            null, null
        );
    }
    
    /**
     * Captures the current screen content as a bitmap
     * 
     * @return Bitmap of the captured screen or null if failed
     */
    public Bitmap captureScreen() {
        Image image = null;
        Bitmap bitmap = null;
        
        try {
            // Acquire latest image
            image = imageReader.acquireLatestImage();
            if (image == null) {
                Log.e(TAG, "Failed to acquire screen image");
                return null;
            }
            
            // Get image dimensions and format
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            
            // Convert Image to Bitmap
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * imageWidth;
            
            // Create bitmap
            bitmap = Bitmap.createBitmap(
                    imageWidth + rowPadding / pixelStride, 
                    imageHeight, 
                    Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            
            // If bitmap dimensions don't match expected dimensions, crop it
            if (bitmap.getWidth() != imageWidth || bitmap.getHeight() != imageHeight) {
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, imageWidth, imageHeight);
            }
            
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error capturing screen: " + e.getMessage());
            if (bitmap != null) {
                bitmap.recycle();
                bitmap = null;
            }
            return null;
        } finally {
            // Always close the image when done
            if (image != null) {
                image.close();
            }
        }
    }
    
    /**
     * Release resources when no longer needed
     */
    public void release() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }
}