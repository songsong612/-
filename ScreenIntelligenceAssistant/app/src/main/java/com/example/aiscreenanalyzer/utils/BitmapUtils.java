package com.example.aiscreenanalyzer.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for working with bitmaps
 */
public class BitmapUtils {

    /**
     * Converts a byte array to a Base64 encoded string
     */
    public static String bytesToBase64(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
    
    /**
     * Generates a hash for a bitmap to use as a cache key
     */
    public static String getBitmapHash(Bitmap bitmap) {
        if (bitmap == null) {
            return "";
        }
        
        try {
            // Create a ByteBuffer for bitmap pixels
            ByteBuffer buffer = ByteBuffer.allocate(bitmap.getWidth() * bitmap.getHeight() * 4);
            bitmap.copyPixelsToBuffer(buffer);
            
            // Get a subsample of pixels for efficiency (every 10th pixel)
            byte[] pixelData = new byte[buffer.capacity() / 10];
            buffer.rewind();
            for (int i = 0; i < pixelData.length; i++) {
                // Skip 9 bytes, read 1
                if (buffer.position() + 9 < buffer.capacity()) {
                    buffer.position(buffer.position() + 9);
                }
                if (buffer.hasRemaining()) {
                    pixelData[i] = buffer.get();
                }
            }
            
            // Create a hash of the pixel data
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(pixelData);
            byte[] digest = md.digest();
            
            // Convert to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(bitmap.getWidth() * bitmap.getHeight());
        }
    }
    
    /**
     * Scales a bitmap to fit within the specified dimensions while maintaining aspect ratio
     */
    public static Bitmap scaleBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        if (bitmap == null) {
            return null;
        }
        
        float width = bitmap.getWidth();
        float height = bitmap.getHeight();
        
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }
        
        float scaleWidth = maxWidth / width;
        float scaleHeight = maxHeight / height;
        float scale = Math.min(scaleWidth, scaleHeight);
        
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
    
    /**
     * Creates a rounded corner bitmap
     */
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float cornerRadius) {
        if (bitmap == null) {
            return null;
        }
        
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), 
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.BLACK);
        canvas.drawRoundRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), 
                cornerRadius, cornerRadius, paint);
        
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        
        return output;
    }
    
    /**
     * Compresses a bitmap to JPEG with the specified quality
     */
    public static byte[] compressBitmap(Bitmap bitmap, int quality) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        return outputStream.toByteArray();
    }
}