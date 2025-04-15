package com.fire.camera.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.WindowManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Tools {
    /**
     * 获得屏幕宽度
     *
     * @param context
     * @return
     */
    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    /**
     * 获得屏幕高度
     *
     * @param context
     * @return
     */
    public static int getScreenHeight(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.heightPixels;
    }

    public static int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);  //+0.5是为了向上取整
    }

    public static int px2dp(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);//+0.5是为了向上取整

    }

    public static void moveFileToDCIM(Context context, String filePath, boolean isVideo) {
        File internalFile = new File(filePath);

        if (!internalFile.exists()) {
            Log.e("MoveFile", "源文件不存在");
            return;
        }
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        // 创建写入 MediaStore 的内容信息
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Pictures");

        ContentResolver resolver = context.getContentResolver();

        Uri uri;
        if (isVideo) {
            uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        } else {
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        }
        if (uri != null) {
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
    }


    /**
     * 找到Size数组中与目标比例接近，且分辨率最大的一项
     *
     * @param sizes       Size数组
     * @param targetRatio 目标比例（宽/高）
     * @return 匹配的最大Size
     */
    public static Size findBestMatchingSize(Size[] sizes, float targetRatio) {
        if (sizes == null || sizes.length == 0) return null;

        final float ASPECT_TOLERANCE = 0.05f; // 可接受的比例误差
        Size bestSize = null;
        int maxPixels = 0;

        for (Size size : sizes) {
            float ratio = (float) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) <= ASPECT_TOLERANCE) {
                int pixels = size.getWidth() * size.getHeight();
                if (pixels > maxPixels) {
                    maxPixels = pixels;
                    bestSize = size;
                }
            }
        }

        return bestSize;
    }

    public static Bitmap setTakePictureOrientation(int id, Bitmap bitmap) {
        if (bitmap == null)
            return null;
        //如果返回的图片宽度小于高度，说明FrameWork层已经做过处理直接返回即可
        if (bitmap.getWidth() < bitmap.getHeight()) {
            return bitmap;
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(id, info);
        return rotatingImageView(id, info.orientation, bitmap);
    }

    private static Bitmap rotatingImageView(int id, int angle, Bitmap bitmap) {
        //矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        //加入翻转 把相机拍照返回照片转正
        if (id == 1) {
            matrix.postScale(-1, 1);
        }
        // 创建新的图片
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static boolean saveBitmapWithCompress(Bitmap bitmap, String path, float compress) {
        try {
            File file = new File(path);
            File parent = file.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(file);
            boolean b = bitmap.compress(Bitmap.CompressFormat.JPEG, (int) (100 * compress), fos);
            fos.flush();
            fos.close();
            return b;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean saveBitmap(Bitmap bitmap, String path) {
        try {
            File file = new File(path);
            File parent = file.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(file);
            boolean b = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            return b;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
