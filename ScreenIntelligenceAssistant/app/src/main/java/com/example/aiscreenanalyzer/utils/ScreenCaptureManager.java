package com.example.aiscreenanalyzer.utils;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.nio.ByteBuffer;

public class ScreenCaptureManager {
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    
    private int width;
    private int height;
    private int density;
    
    public ScreenCaptureManager(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
        
        // Get display metrics
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) mediaProjection.getSystemService(WindowManager.class);
        Display display = windowManager.getDefaultDisplay();
        display.getMetrics(metrics);
        
        // Set width, height, and density
        this.width = metrics.widthPixels;
        this.height = metrics.heightPixels;
        this.density = metrics.densityDpi;
    }
    
    public Bitmap captureScreen() {
        if (mediaProjection == null) {
            return null;
        }
        
        try {
            // Create image reader
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
            
            // Create virtual display
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "ScreenCapture",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(),
                    null,
                    null
            );
            
            // Capture image
            Image image = null;
            
            // Try a few times to acquire the latest image
            for (int i = 0; i < 10; i++) {
                image = imageReader.acquireLatestImage();
                if (image != null) {
                    break;
                }
                Thread.sleep(100); // Wait a bit if no image is available
            }
            
            if (image == null) {
                return null;
            }
            
            // Get image dimensions
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            
            // Get image planes
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * imageWidth;
            
            // Create bitmap
            Bitmap bitmap = Bitmap.createBitmap(
                    imageWidth + rowPadding / pixelStride,
                    imageHeight,
                    Bitmap.Config.ARGB_8888
            );
            bitmap.copyPixelsFromBuffer(buffer);
            
            // Crop bitmap to exact screen size
            Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, imageWidth, imageHeight);
            bitmap.recycle();
            
            // Close image and release resources
            image.close();
            
            return croppedBitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            // Release resources
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
