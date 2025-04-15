package com.fire.camera.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.SeekBar;

import androidx.annotation.ColorInt;

import com.fire.camera.R;
import com.fire.camera.databinding.ItemVideoPreviewBinding;

import java.lang.ref.WeakReference;


public class VideoPopupWindow extends PopupWindow implements View.OnClickListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnInfoListener, SeekBar.OnSeekBarChangeListener {
    private static final int MSG_HIDE_CONTROL = 0x01;
    private static final int MSG_UPDATE_PLAY_PROGRESS = 0x02;
    private final ItemVideoPreviewBinding binding;
    private final VideoPopupWindow.mHandler mHandler;
    private OnPreviewDoneListener onPreviewDoneListener;
    private OnPreviewBackListener onPreviewBackListener;
    private int videoHeight;
    private int videoWidth;
    Context context;


    public VideoPopupWindow(Context context, String videoPath) {
        super(context);
        this.context = context;
        setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        setHeight(WindowManager.LayoutParams.MATCH_PARENT);
        binding = ItemVideoPreviewBinding.inflate(LayoutInflater.from(context));
        setContentView(binding.getRoot());
        setClippingEnabled(false);
        mHandler = new mHandler(this);
        binding.ivVideoFunction.setOnClickListener(this::onClick);
        binding.llPreviewBack.setOnClickListener(this::onClick);
        binding.tvPreviewDone.setOnClickListener(this::onClick);
        binding.vvPreview.setOnClickListener(this::onClick);
        binding.vvPreview.setOnPreparedListener(this);
        binding.vvPreview.setOnCompletionListener(this);
        binding.vvPreview.setOnInfoListener(this);
        binding.vvPreview.setVideoPath(videoPath);
    }

    public void setVideoPath(String videoPath) {
        binding.vvPreview.setVideoPath(videoPath);
    }

    public void cancelSaveVideo() {
        if (onPreviewBackListener != null) {
            onPreviewBackListener.onBackClick();
        }
    }


    public interface OnPreviewBackListener {
        void onBackClick();
    }

    public void setOnPreviewBackListener(OnPreviewBackListener onPreviewBackListener) {
        this.onPreviewBackListener = onPreviewBackListener;
    }

    public interface OnPreviewDoneListener {
        void onDoneClick(int videoHeight, int videoWidth);
    }

    public void setOnPreviewDoneListener(OnPreviewDoneListener onPreviewDoneListener) {
        this.onPreviewDoneListener = onPreviewDoneListener;
    }

    public void setBackImageColor(@ColorInt int color){
        binding.ivBack.setImageTintList(ColorStateList.valueOf(color));
    }

    public void setBackTextColor(@ColorInt int color){
        binding.tvBack.setTextColor(color);
    }

    public void setDoneTextColor(@ColorInt int color){
        binding.tvPreviewDone.setTextColor(color);
    }

    public void setDoneText(String text){
        binding.tvPreviewDone.setText(text);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_video_function) {
            if (binding.vvPreview.isPlaying()) {
                binding.vvPreview.pause();
                binding.ivVideoFunction.setImageResource(R.drawable.ic_camera_preview_play);
            } else {
                binding.vvPreview.start();
                binding.ivVideoFunction.setImageResource(R.drawable.ic_camera_preview_pause);
                mHandler.sendEmptyMessageDelayed(MSG_HIDE_CONTROL, 1000);
            }
        } else if (id == R.id.ll_preview_back) {
            if (onPreviewBackListener != null) {
                onPreviewBackListener.onBackClick();
            }
            dismiss();
        } else if (id == R.id.tv_preview_done) {
            if (onPreviewDoneListener != null) {
                if (binding.vvPreview.isPlaying()) {
                    binding.vvPreview.pause();
                }
                // 移除隐藏按钮handler
                mHandler.removeMessages(MSG_HIDE_CONTROL);
                // 显示按钮
                binding.ivVideoFunction.setVisibility(View.VISIBLE);
                // 设置进度条按钮
                binding.ivVideoFunction.setImageResource(R.drawable.loading_process_rotate);
                onPreviewDoneListener.onDoneClick(videoHeight, videoWidth);
            }
        } else if (id == R.id.vv_preview) {
            binding.ivVideoFunction.setVisibility(binding.ivVideoFunction.isShown() ? View.GONE : View.VISIBLE);
            if (binding.ivVideoFunction.isShown()) {
                if (binding.vvPreview.isPlaying()) {
                    mHandler.sendEmptyMessageDelayed(MSG_HIDE_CONTROL, 1000);
                }
            } else {
                mHandler.removeMessages(MSG_HIDE_CONTROL);
            }
        }
    }

    // 防止handler内存泄漏
    private static class mHandler extends Handler {
        private WeakReference<VideoPopupWindow> reference;

        mHandler(VideoPopupWindow videoPopupWindow) {
            reference = new WeakReference<>(videoPopupWindow);
        }

        @Override
        public void handleMessage(Message msg) {
            VideoPopupWindow videoPopupWindow = reference.get();
            if (videoPopupWindow != null) {
                switch (msg.what) {
                    case MSG_HIDE_CONTROL:
                        videoPopupWindow.binding.ivVideoFunction.setVisibility(View.GONE);
                        break;
                    case MSG_UPDATE_PLAY_PROGRESS:
                        videoPopupWindow.updatePlayProgress();
                        break;
                }
            }
        }
    }

    public void setTopBackButtonGoneOrVisible(int visible) {
        binding.llPreviewBack.setVisibility(visible);
    }

    public void setTopFinishButtonGoneOrVisible(int visible) {
        binding.tvPreviewDone.setVisibility(visible);

    }

    public void setBackground(Drawable background) {
        binding.getRoot().setBackground(background);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        videoHeight = mp.getVideoHeight();
        videoWidth = mp.getVideoWidth();
        calculateView();
        binding.sbSeekBar.setMax(binding.vvPreview.getDuration());
        binding.tvTimeCurrent.setText("00:00");
        binding.vvPreview.start();
        binding.tvTimeTotal.setText(formatVideoDuration(binding.vvPreview.getDuration()));
        updatePlayProgress();
//        mHandler.sendEmptyMessageDelayed(MSG_HIDE_CONTROL, 1000);
    }

    public void updatePlayProgress() {
        binding.tvTimeCurrent.setText(formatVideoDuration(binding.vvPreview.getCurrentPosition()));
        binding.sbSeekBar.setProgress(binding.vvPreview.getCurrentPosition());
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_PLAY_PROGRESS, 500);
    }

    private void calculateView() {
        int videoViewWidth = binding.vvPreview.getWidth();
        int videoViewHeight = binding.vvPreview.getHeight();

        if (videoWidth < videoViewWidth && videoHeight >= videoViewHeight) {
            float rate = (float) videoHeight / videoWidth;
            float newVideoHeight = videoViewWidth * rate;
            resetVideoViewHeight((int) newVideoHeight);
        } else if (videoWidth > videoViewWidth && videoHeight >= videoViewHeight) {
            float rate = (float) videoHeight / videoWidth;
            float newVideoHeight = videoViewWidth * rate;
            resetVideoViewHeight((int) newVideoHeight);
        }
    }

    private void resetVideoViewHeight(int newHeight) {
        ViewGroup.LayoutParams layoutParams = binding.vvPreview.getLayoutParams();
        layoutParams.height = newHeight;
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        binding.vvPreview.setLayoutParams(layoutParams);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        binding.ivVideoFunction.setImageResource(R.drawable.ic_camera_preview_play);
        binding.ivVideoFunction.setVisibility(View.VISIBLE);
    }


    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
//        videoHeight = mp.getVideoHeight();
//        videoWidth = mp.getVideoWidth();
//        ViewGroup.LayoutParams layoutParams = binding.vvPreview.getLayoutParams();
//        layoutParams.height = videoHeight;
//        layoutParams.width = videoWidth;
//        binding.vvPreview.setLayoutParams(layoutParams);
        return false;
    }

    /**
     * SeekBar拖动监听
     *
     * @param seekBar
     * @param progress
     * @param fromUser
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {// 用户拖动
            binding.vvPreview.seekTo(progress);
            binding.tvTimeCurrent.setText(formatVideoDuration(progress));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    /**
     * 将long类型的时间转为01:22:33
     *
     * @param duration
     * @return
     */
    public static String formatVideoDuration(long duration) {
        int HOUR = 60 * 60 * 1000;//1小时所占的毫秒数
        int MINUTE = 60 * 1000;//1分钟所占的毫秒数
        int SECOND = 1000;//1秒

        //1.先算出多少小时，然后拿剩余的时间去算分钟
        int hour = (int) (duration / HOUR);//得到多少小时
        long remaintTime = duration % HOUR;//算完小时剩余的时间
        //2.算出多少分钟后，拿剩余的时间去算秒
        int minute = (int) (remaintTime / MINUTE);//得到多少分钟
        remaintTime = remaintTime % MINUTE;//算完分钟得到的时间
        //3.算出多少秒
        int second = (int) (remaintTime / SECOND);//得到多少秒

        //字符串格式的过程
        if (hour == 0) {
            //转为02:33格式
            return String.format("%02d:%02d", minute, second);
        } else {
            return String.format("%02d:%02d:%02d", hour, minute, second);
        }
    }

    public static VideoPopupWindow videoPopupWindow;

    /**
     * 播放视频
     */
    public static void showVideoPreview(Context context, String videoPath
            , View atLocationView, int x, int y) {
        videoPopupWindow = new VideoPopupWindow(context, videoPath);
        videoPopupWindow.setAnimationStyle(R.style.PopupAnimStyle);
        videoPopupWindow.setBackgroundDrawable(new BitmapDrawable());
        videoPopupWindow.showAtLocation(atLocationView, Gravity.CENTER, x, y);
//        videoPopupWindow.setTopBackButtonGoneOrVisible(View.GONE);
//        videoPopupWindow.setTopFinishButtonGoneOrVisible(View.GONE);
        videoPopupWindow.setBackImageColor(Color.parseColor("#000000"));
        videoPopupWindow.setBackTextColor(Color.parseColor("#000000"));
        videoPopupWindow.setDoneTextColor(Color.parseColor("#000000"));
        videoPopupWindow.setOnPreviewDoneListener(
                (videoHeight, videoWidth) -> dismissVideoPreview());
        videoPopupWindow.setBackground(context.getDrawable(R.color.white));
    }

    public static void dismissVideoPreview() {
        if (null != videoPopupWindow && videoPopupWindow.isShowing()) {
            videoPopupWindow.dismiss();
            videoPopupWindow = null;
        }
    }
}
