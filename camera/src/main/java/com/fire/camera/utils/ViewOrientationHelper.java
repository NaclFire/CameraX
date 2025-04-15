package com.fire.camera.utils;

import android.app.Activity;
import android.content.Context;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.RotateAnimation;
import java.util.List;

public class ViewOrientationHelper {

    private final Context context;
    private final List<View> targetViews;
    private final OrientationEventListener orientationListener;
    private int lastRotation = 0;
    private int currentRotation = 0;

    public ViewOrientationHelper(Activity activity, List<View> targetViews) {
        this.context = activity;
        this.targetViews = targetViews;

        orientationListener = new OrientationEventListener(context) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) return;

                int deviceAngle = getNearestOrientation(orientation);
                int screenRotation = getScreenRotationAngle(activity);
                int targetAngle = (360 - deviceAngle + screenRotation) % 360;

                if (targetAngle != lastRotation) {
                    rotateViews(targetAngle);  // 对所有 Views 进行旋转
                    lastRotation = targetAngle;
                }
            }
        };
    }

    // 启动监听器
    public void start() {
        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable();
        }
    }

    // 停止监听器
    public void stop() {
        orientationListener.disable();
    }

    public int getCurrentRotation(){
        return currentRotation;
    }

    // 获取最近的角度
    private int getNearestOrientation(int orientation) {
        if ((orientation >= 0 && orientation < 45) || (orientation >= 315)) {
            return 0;
        } else if (orientation >= 45 && orientation < 135) {
            return 90;
        } else if (orientation >= 135 && orientation < 225) {
            return 180;
        } else {
            return 270;
        }
    }

    // 获取当前屏幕的旋转角度
    private int getScreenRotationAngle(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                return 0;
        }
    }

    // 对所有 View 执行旋转动画
    private void rotateViews(float targetAngle) {
        for (View view : targetViews) {
            rotateView(view, lastRotation, targetAngle);
        }
    }

    // 执行旋转动画
    private void rotateView(View view, float fromDegrees, float toDegrees) {
        currentRotation = (int) toDegrees;
        RotateAnimation rotate = new RotateAnimation(
                fromDegrees,
                toDegrees,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setDuration(300);
        rotate.setFillAfter(true); // 保持旋转后的角度
        view.startAnimation(rotate);
    }
}

