package com.fire.camera.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.view.PreviewView;

public class PreviewContainerView extends FrameLayout {

    private static final float ASPECT_RATIO = 9f / 16f; // 宽:高

    private PreviewView previewView;

    public PreviewContainerView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public PreviewContainerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PreviewContainerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        previewView = new PreviewView(context);
        previewView.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));
        addView(previewView);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        // 高度 = 宽度 * (16 / 9)
        int height = width * 16 / 9;
        int finalWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int finalHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        super.onMeasure(finalWidthSpec, finalHeightSpec);
    }

    public PreviewView getPreviewView() {
        return previewView;
    }
}
