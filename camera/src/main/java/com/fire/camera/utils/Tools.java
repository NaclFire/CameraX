package com.fire.camera.utils;

import static com.fire.camera.CameraXSetting.CAMERA_SAVE_FOLDER;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowMetrics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Tools {
    public static Size getScreenSize(Context context) {
        DisplayMetrics realMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11 (API 30) 及以上使用新的方式
                WindowMetrics windowMetrics = windowManager.getCurrentWindowMetrics();
                Rect bounds = windowMetrics.getBounds();
                return new Size(bounds.width(), bounds.height());

            } else {
                // Android 10 及以下
                Display display = windowManager.getDefaultDisplay();
                display.getRealMetrics(realMetrics);
                return new Size(realMetrics.widthPixels, realMetrics.heightPixels);
            }
        } else {
            DisplayMetrics dm = new DisplayMetrics();
            ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(dm);
            return new Size(dm.widthPixels, dm.heightPixels);
        }
    }

    public static int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);  //+0.5是为了向上取整
    }

    public static int px2dp(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);//+0.5是为了向上取整

    }

    public static Uri moveFileToDCIM(Context context, String filePath, boolean isVideo) {
        File internalFile = new File(filePath);

        if (!internalFile.exists()) {
            Log.e("MoveFile", "源文件不存在");
            return null;
        }
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        // 创建写入 MediaStore 的内容信息
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        if (isVideo) {
            values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        } else {
            values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        }
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + File.separator + CAMERA_SAVE_FOLDER);
        ContentResolver resolver = context.getContentResolver();

        Uri uri;
        if (isVideo) {
            uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        } else {
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        }
        if (uri != null) {
            Log.e("Tools", "moveFileToDCIM: " + uri);
            try (
                    OutputStream out = resolver.openOutputStream(uri);
                    InputStream in = new FileInputStream(internalFile)
            ) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();

                // 可选：删除原始文件
                internalFile.delete();

                Log.i("MoveFile", "移动成功！");
            } catch (IOException e) {
                Log.e("MoveFile", "移动失败：" + e.getMessage());
            }
        } else {
            Log.e("MoveFile", "无法插入到 MediaStore");
        }
        return uri;
    }
}
