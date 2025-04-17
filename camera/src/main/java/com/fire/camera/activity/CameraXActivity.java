package com.fire.camera.activity;

import static com.fire.camera.CameraXSetting.CAMERA_ORIENTATION_HORIZONTAL;
import static com.fire.camera.CameraXSetting.CAMERA_ORIENTATION_VERTICAL;
import static com.fire.camera.CameraXSetting.CAMERA_SAVE_FOLDER;
import static com.fire.camera.CameraXSetting.CAMERA_VIDEO_MAX_TIME;
import static com.fire.camera.CameraXSetting.CAMERA_VIDEO_MIN_TIME;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.lifecycle.LiveData;

import com.bumptech.glide.Glide;
import com.fire.camera.R;
import com.fire.camera.bean.CameraResultBean;
import com.fire.camera.databinding.ActivityCameraXBinding;
import com.fire.camera.utils.PermissionHelper;
import com.fire.camera.utils.Tools;
import com.fire.camera.utils.ViewOrientationHelper;
import com.fire.camera.view.VideoPopupWindow;
import com.fire.camera.widget.CircleProgressButtonView;
import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CameraXActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = CameraXActivity.class.getSimpleName();
    private final String[] cameraAndAudioPermissions = new String[]
            {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};
    private static final int REQUEST_CAMERA_AND_AUDIO_PERMISSION = 0x0001;
    private CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
    private final int[] flashModes = {ImageCapture.FLASH_MODE_OFF, ImageCapture.FLASH_MODE_AUTO, ImageCapture.FLASH_MODE_ON};
    //    public final String CAMERA_X_DEFAULT_MEDIA_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "DCIM" + File.separator + "Pictures" + File.separator;
    private com.fire.camera.databinding.ActivityCameraXBinding binding;
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA);

    /**
     * 前置后置相机
     */
    private boolean isBackCamera = true;
    private boolean isTakePhoto;
    private String tempPath;
    private String defaultPath;
    //    private File cameraFolder;
    private File cameraCacheFolder;
    private String imagePath;
    private String videoPath;
    private boolean isSaveTempRecord;
    private ViewOrientationHelper orientationHelper;
    private static OnCameraCallback onCameraCallback;
    private Preview preview;


    public static class Builder {
        /**
         * 保存到DCIM下的文件夹名称，默认为应用名
         */
        public Builder setSaveFolder(String folderName) {
            CAMERA_SAVE_FOLDER = folderName;
            return this;
        }

        /**
         * 最长录制时间，默认30，单位秒
         */
        public Builder setMaxTime(int maxTime) {
            CAMERA_VIDEO_MAX_TIME = maxTime;
            return this;
        }

        /**
         * 最短录制时间，默认0（不限制最短时间），单位秒
         */
        public Builder setMinTime(int minTime) {
            CAMERA_VIDEO_MIN_TIME = minTime;
            return this;
        }

        public Builder setOnCameraCallback(OnCameraCallback onCameraCallback) {
            CameraXActivity.onCameraCallback = onCameraCallback;
            return this;
        }

        public void build(Context context) {
            if (context != null) {
                Intent intent = new Intent(context, CameraXActivity.class);
                context.startActivity(intent);
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        binding = ActivityCameraXBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (!PermissionHelper.hasStoragePermissions(this)) {
            PermissionHelper.requestStoragePermissions(this);
        }
        cameraCacheFolder = new File(getExternalFilesDir("Cache").getAbsolutePath() + "/Media/");
        if (!cameraCacheFolder.exists()) {
            cameraCacheFolder.mkdirs();
        }
        if (TextUtils.isEmpty(CAMERA_SAVE_FOLDER)) {
            try {
                ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), 0);
                CharSequence appName = getPackageManager().getApplicationLabel(ai);
                CAMERA_SAVE_FOLDER = appName.toString();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        defaultPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "DCIM" + File.separator + CAMERA_SAVE_FOLDER;
        initView();
        initClickListener();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQUEST_CODE_STORAGE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            if (granted) {
                // 权限已授予
                initCamera();
            } else {
                // 拒绝授权，开弹窗跳询问是否跳设置-权限管理界面
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setMessage("应用需要您的相机、麦克风、文件操作权限，请到设置-权限管理中授权。")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.addCategory(Intent.CATEGORY_DEFAULT);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                startActivity(intent);
                            }
                        }).setCancelable(false)
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(CameraXActivity.this, "您没有允许权限，此功能不能正常使用", Toast.LENGTH_SHORT).show();
                            }
                        });
                builder.create().show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (orientationHelper != null) {
            orientationHelper.start();
        }
        initCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (orientationHelper != null) {
            orientationHelper.stop();
        }
    }

    private GestureDetector gestureDetector;

    private void initView() {
        int imageHeight = Tools.getScreenSize(this).getWidth() * 16 / 9;
        if (Tools.getScreenSize(this).getHeight() - imageHeight > Tools.dp2px(this, 100)) {
            ViewGroup.LayoutParams layoutParams = binding.rlBottomTakeButton.getLayoutParams();
            layoutParams.height = Tools.getScreenSize(this).getHeight() - imageHeight;
            binding.rlBottomTakeButton.setLayoutParams(layoutParams);
            layoutParams = binding.llConfirmButtonLayout.getLayoutParams();
            layoutParams.height = Tools.getScreenSize(this).getHeight() - imageHeight;
            binding.llConfirmButtonLayout.setLayoutParams(layoutParams);
        }
        binding.previewView.setScaleType(PreviewView.ScaleType.FIT_START);
        binding.previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
        gestureDetector = new GestureDetector(this, onGestureListener);
        gestureDetector.setOnDoubleTapListener(onDoubleTapListener);
        binding.ivCameraFlashButton.setTag(0);
        List<View> views = new ArrayList<>();
        views.add(binding.ivCameraFunctionButton);
        views.add(binding.ivCameraFlashButton);
        views.add(binding.btRecord);
        orientationHelper = new ViewOrientationHelper(this, views);
        orientationHelper.setOnOrientationChangedListener(new ViewOrientationHelper.OnOrientationChangedListener() {
            @Override
            public void onOrientationChanged(int targetAngle) {
                int rotation;
                if (targetAngle == 0) {
                    rotation = Surface.ROTATION_0;
                } else if (targetAngle == 90) {
                    rotation = Surface.ROTATION_90;
                } else if (targetAngle == 180) {
                    rotation = Surface.ROTATION_180;
                } else {
                    rotation = Surface.ROTATION_270;
                }
                if (videoCapture != null) {
                    videoCapture.setTargetRotation(rotation);
                }
                if (imageCapture != null) {
                    imageCapture.setTargetRotation(rotation);
                }
                if (preview != null) {
                    preview.setTargetRotation(rotation);
                }
            }
        });
    }

    private void initClickListener() {
        binding.ivCameraFlashButton.setOnClickListener(this::onClick);
        binding.ivCameraFunctionButton.setOnClickListener(this::onClick);
        binding.tvCancel.setOnClickListener(this::onClick);
        binding.tvOk.setOnClickListener(this::onClick);
        binding.btRecord.setOnClickListener(this::takePhoto);
        binding.btRecord.setOnLongClickListener(new CircleProgressButtonView.OnLongClickListener() {
            @Override
            public void onLongClick() {
                startRecord();
            }

            @Override
            public void onNoMinRecord(int currentTime) {
                Toast.makeText(CameraXActivity.this, "未达到最短录制时间", Toast.LENGTH_SHORT).show();
                isSaveTempRecord = false;
                try {
                    if (currentRecording != null) {
                        currentRecording.stop();
                        currentRecording = null;
                    }
                    new File(tempPath).delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onRecordFinishedListener() {
                isSaveTempRecord = true;
                try {
                    if (currentRecording != null) {
                        currentRecording.stop();
                        currentRecording = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(CameraXActivity.this, "视频保存失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_camera_flash_button) {
            int tag = (int) v.getTag();
            switch (tag) {
                case 0:
                    v.setTag(1);
                    binding.ivCameraFlashButton.setImageResource(R.drawable.ic_camera_flash_auto);
                    imageCapture.setFlashMode(flashModes[1]);
                    break;
                case 1:
                    v.setTag(2);
                    binding.ivCameraFlashButton.setImageResource(R.drawable.ic_camera_flash_on);
                    imageCapture.setFlashMode(flashModes[2]);
                    break;
                case 2:
                    v.setTag(3);
                    binding.ivCameraFlashButton.setImageResource(R.drawable.ic_camera_flash_light);
                    cameraControl.enableTorch(true);
                    break;
                case 3:
                    v.setTag(0);
                    cameraControl.enableTorch(false);
                    binding.ivCameraFlashButton.setImageResource(R.drawable.ic_camera_flash_off);
                    break;
                default:
                    break;
            }
        } else if (id == R.id.iv_camera_function_button) {
            if (isBackCamera) {
                cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
            } else {
                cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
            }
            isBackCamera = !isBackCamera;
            initCamera();
        } else if (id == R.id.tv_cancel) {
            cancelSavePhoto();
        } else if (id == R.id.tv_ok) {
            Uri uri = Tools.moveFileToDCIM(this, tempPath, false);
            CameraResultBean cameraResultBean = new CameraResultBean();
            cameraResultBean.setUri(uri);
            cameraResultBean.setPath(imagePath);
            cameraResultBean.setType(CameraResultBean.CAMERA_RESULT_TYPE_PHOTO);
            if (onCameraCallback != null) {
                onCameraCallback.onCameraResult(cameraResultBean);
            }
            finish();
        }
    }

    private VideoCapture<Recorder> videoCapture;
    private ImageCapture imageCapture;
    private Camera camera;
    private CameraInfo cameraInfo;
    private CameraControl cameraControl;
    private LiveData<ZoomState> zoomState;
    private float maxZoomRatio;
    private float minZoomRatio;

    private void initCamera() {
        ListenableFuture<ProcessCameraProvider> instance = ProcessCameraProvider.getInstance(this);
        instance.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = instance.get();
                Preview.Builder builder = new Preview.Builder();
                ResolutionSelector previewResolutionSelector = new ResolutionSelector.Builder()
                        .setAspectRatioStrategy(
                                new AspectRatioStrategy(AspectRatio.RATIO_16_9, AspectRatioStrategy.FALLBACK_RULE_AUTO)
                        )
                        .build();
                preview = builder.setResolutionSelector(previewResolutionSelector).build();
                cameraProvider.unbindAll();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());
                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .setAspectRatio(AspectRatio.RATIO_16_9)
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);
                ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
                        .setAspectRatioStrategy(
                                new AspectRatioStrategy(AspectRatio.RATIO_16_9, AspectRatioStrategy.FALLBACK_RULE_AUTO)
                        )
                        .build();
                imageCapture = new ImageCapture.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build();
                camera = cameraProvider.bindToLifecycle(CameraXActivity.this, cameraSelector, preview, videoCapture, imageCapture);
                cameraInfo = camera.getCameraInfo();
                cameraControl = camera.getCameraControl();
                zoomState = cameraInfo.getZoomState();
                maxZoomRatio = zoomState.getValue().getMaxZoomRatio();
                minZoomRatio = zoomState.getValue().getMinZoomRatio();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());
                binding.previewView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return gestureDetector.onTouchEvent(event);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * 注释：拍照并保存图片到相册
     */
    private void takePhoto() {
        // 先将图片保存在内部，用户点击保存再移动到外部
        String fileName = "IMG_" + simpleDateFormat.format(new Date()) + ".jpg";
        tempPath = cameraCacheFolder.getAbsolutePath() + File.separator + fileName;
        imagePath = defaultPath + File.separator + fileName;
        File imageFile = new File(tempPath);
        ImageCapture.OutputFileOptions imageOutputFileOptions = new ImageCapture.OutputFileOptions.Builder(imageFile).build();
        if (imageCapture != null) {
            imageCapture.takePicture(imageOutputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    isTakePhoto = true;
                    handleImage(outputFileResults.getSavedUri());
                    Glide.with(CameraXActivity.this).load(outputFileResults.getSavedUri()).into(binding.ivTakePhotoPreview);
                    // 隐藏拍照界面
                    binding.llPhotoLayout.setVisibility(View.INVISIBLE);
                    // 显示预览
                    fadeInView(binding.llConfirmLayout, 300);
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    isTakePhoto = false;
                    Toast.makeText(CameraXActivity.this, "拍照失败", Toast.LENGTH_SHORT).show();
                    exception.printStackTrace();
                }
            });
        }
    }

    private Recording currentRecording;

    @SuppressLint("MissingPermission")
    private void startRecord() {
        String fileName = "VID_" + simpleDateFormat.format(new Date()) + ".mp4";
        // 先将保存在内部，用户点击保存再移动到外部
        tempPath = cameraCacheFolder.getAbsolutePath() + File.separator + fileName;
        videoPath = defaultPath + File.separator + fileName;
        FileOutputOptions outputOptions = new FileOutputOptions.Builder(new File(tempPath)).build();
        // 保存到默认位置
//        ContentValues contentValues = new ContentValues();
//        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
//        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
//        MediaStoreOutputOptions outputOptions = new MediaStoreOutputOptions.Builder(
//                getContentResolver(),
//                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
//        ).setContentValues(contentValues).build();
        PendingRecording pendingRecording = videoCapture.getOutput()
                .prepareRecording(this, outputOptions)
                .withAudioEnabled();
        if (videoCapture != null) {
            currentRecording = pendingRecording.start(ContextCompat.getMainExecutor(this), event -> {
                if (event instanceof VideoRecordEvent.Start) {
//                    Toast.makeText(this, "开始录制", Toast.LENGTH_SHORT).show();
                } else if (event instanceof VideoRecordEvent.Finalize) {
//                    Toast.makeText(this, "录制完成", Toast.LENGTH_SHORT).show();
                    if (isSaveTempRecord) {
                        binding.previewView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                showVideoPreview();
                            }
                        }, 500);
                    }
                    currentRecording = null;
                }
            });
        }
    }

    private VideoPopupWindow videoPopupWindow;

    private void showVideoPreview() {
        Log.e(TAG, "showVideoPreview: ");
        if (videoPopupWindow == null) {
            videoPopupWindow = new VideoPopupWindow(this, tempPath);
        } else {
            videoPopupWindow.setVideoPath(tempPath);
        }
        videoPopupWindow.setAnimationStyle(R.style.PopupAnimStyle);
        videoPopupWindow.setBackgroundDrawable(new BitmapDrawable());
        videoPopupWindow.showAtLocation(binding.getRoot(), Gravity.CENTER, 0, 0);
        videoPopupWindow.setOnPreviewBackListener(() -> {
            File file = new File(tempPath);
            if (file.exists()) {
                boolean isDelete = file.delete();
                if (isDelete) {
                    Log.e("CameraXActivity", "删除成功");
                }
            }
        });
        videoPopupWindow.setOnPreviewDoneListener((videoHeight, videoWidth) -> {
            // 从内部移动到外部
            Uri uri = Tools.moveFileToDCIM(this, tempPath, true);
            CameraResultBean cameraResultBean = new CameraResultBean();
            cameraResultBean.setPath(videoPath);
            cameraResultBean.setUri(uri);
            cameraResultBean.setType(CameraResultBean.CAMERA_RESULT_TYPE_VIDEO);
            if (onCameraCallback != null) {
                onCameraCallback.onCameraResult(cameraResultBean);
            }
            finish();
        });
    }

    /**
     * 缩放相关
     */
    private float currentDistance = 0;
    private float lastDistance = 0;
    GestureDetector.OnGestureListener onGestureListener = new GestureDetector.OnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // 大于两个触摸点
            if (e2.getPointerCount() >= 2) {
                //event中封存了所有屏幕被触摸的点的信息，第一个触摸的位置可以通过event.getX(0)/getY(0)得到
                float offSetX = e2.getX(0) - e2.getX(1);
                float offSetY = e2.getY(0) - e2.getY(1);
                //运用三角函数的公式，通过计算X,Y坐标的差值，计算两点间的距离
                currentDistance = (float) Math.sqrt(offSetX * offSetX + offSetY * offSetY);
                if (lastDistance == 0) {//如果是第一次进行判断
                    lastDistance = currentDistance;
                } else {
                    if (currentDistance - lastDistance > 10) {
                        // 放大
                        float zoomRatio = zoomState.getValue().getZoomRatio();
                        if (zoomRatio < maxZoomRatio) {
                            cameraControl.setZoomRatio((float) (zoomRatio + 0.1));
                        }
                    } else if (lastDistance - currentDistance > 10) {
                        // 缩小
                        float zoomRatio = zoomState.getValue().getZoomRatio();
                        if (zoomRatio > minZoomRatio) {
                            cameraControl.setZoomRatio((float) (zoomRatio - 0.1));
                        }
                    }
                }
                //在一次缩放操作完成后，将本次的距离赋值给lastDistance，以便下一次判断
                //但这种方法写在move动作中，意味着手指一直没有抬起，监控两手指之间的变化距离超过10
                //就执行缩放操作，不是在两次点击之间的距离变化来判断缩放操作
                //故这种将本次距离留待下一次判断的方法，不能在两次点击之间使用
                lastDistance = currentDistance;
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            currentDistance = 0;
            lastDistance = 0;
            return true;
        }
    };
    GestureDetector.OnDoubleTapListener onDoubleTapListener = new GestureDetector.OnDoubleTapListener() {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            FocusMeteringAction build = new FocusMeteringAction.Builder(binding.previewView.getMeteringPointFactory().createPoint(e.getX(), e.getY())).build();
            showTapView((int) e.getX(), (int) e.getY());
            camera.getCameraControl().startFocusAndMetering(build);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // 双击放大缩小
            float zoomRatio = zoomState.getValue().getZoomRatio();
            if (zoomRatio > minZoomRatio) {
                cameraControl.setLinearZoom(0f);
            } else {
                cameraControl.setLinearZoom(0.5f);
            }
            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return false;
        }
    };
    PopupWindow popupWindow;
    ImageView imageView;

    private void showTapView(int x, int y) {
        if (popupWindow == null) {
            popupWindow = new PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        if (imageView == null) {
            imageView = new ImageView(this);
            imageView.setImageResource(R.drawable.ic_camera_focus_view);
        }
        int height = imageView.getHeight() / 2;
        int width = imageView.getWidth() / 2;
        popupWindow.setContentView(imageView);
        popupWindow.showAsDropDown(binding.previewView, x - width, y - height);
        binding.previewView.postDelayed(new Runnable() {
            @Override
            public void run() {
                popupWindow.dismiss();
            }
        }, 600);
        binding.previewView.playSoundEffect(SoundEffectConstants.CLICK);
    }

    private void handleImage(Uri uri) {
        int orientation = CAMERA_ORIENTATION_VERTICAL;
        if (orientationHelper != null) {
            int rotationDegree = orientationHelper.getCurrentRotation();
            if (rotationDegree == 90 || rotationDegree == 270) {
                orientation = CAMERA_ORIENTATION_HORIZONTAL;
            }
        }
        if (orientation == CAMERA_ORIENTATION_HORIZONTAL) {
            binding.ivTakePhotoPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        } else {
            binding.ivTakePhotoPreview.setScaleType(ImageView.ScaleType.FIT_START);
        }
        if (!isBackCamera) {
            Bitmap bitmap = null;
            try {
                // 从文件读取 Bitmap
                InputStream inputStream = getContentResolver().openInputStream(uri);
                bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                // 获取 EXIF 角度
                int rotation = getExifRotation(uri);
                Log.e(TAG, "handleImage: rotation = " + rotation);
                Matrix matrix = new Matrix();
                matrix.setScale(-1, 1);// 左右镜像
                matrix.postRotate(360 - rotation);// 旋转
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                // 保存回原路径
                OutputStream outputStream = getContentResolver().openOutputStream(uri, "w");
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        }
    }

    public int getExifRotation(Uri imageUri) {
        InputStream input = null;
        try {
            input = getContentResolver().openInputStream(imageUri);
            if (input == null) return 0;
            ExifInterface exif = new ExifInterface(input);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void fadeInView(View view, long duration) {
        // 如果当前是可见的就不用动画
        if (view.getVisibility() == View.VISIBLE) return;

        view.setAlpha(0f); // 从透明开始
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1f) // 变为不透明
                .setDuration(duration)
                .setListener(null); // 没有额外监听
    }

    private void cancelSavePhoto() {
        // 显示拍照界面
        binding.llPhotoLayout.setVisibility(View.VISIBLE);
        // 隐藏预览
        binding.llConfirmLayout.setVisibility(View.GONE);
        // 删除临时照片
        new File(tempPath).delete();
    }

    public interface OnCameraCallback {
        void onCameraResult(CameraResultBean cameraResultBean);
    }

    @Override
    public void onBackPressed() {
        if (isTakePhoto) {
            cancelSavePhoto();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoPopupWindow != null && videoPopupWindow.isShowing()) {
            videoPopupWindow.dismiss();
        }
        onCameraCallback = null;
    }
}
