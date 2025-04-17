package com.fire.camerax;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.fire.camera.activity.CameraXActivity;
import com.fire.camera.bean.CameraResultBean;
import com.fire.camerax.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btStartCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new CameraXActivity.Builder()
                        .setOnCameraCallback(new CameraXActivity.OnCameraCallback() {
                            @Override
                            public void onCameraResult(CameraResultBean cameraResultBean) {
                                Log.e("MainActivity", "cameraResultBean.getPath(): " + cameraResultBean.getPath());
                                Log.e("MainActivity", "cameraResultBean.getUri(): " + cameraResultBean.getUri());
                                if (cameraResultBean.getType() == CameraResultBean.CAMERA_RESULT_TYPE_PHOTO) {
                                    binding.imageView.setVisibility(View.VISIBLE);
                                    binding.videoView.setVisibility(View.GONE);
                                    Glide.with(MainActivity.this).load(cameraResultBean.getUri()).into(binding.imageView);
                                } else {
                                    binding.imageView.setVisibility(View.GONE);
                                    binding.videoView.setVisibility(View.VISIBLE);
                                    binding.videoView.setVideoURI(cameraResultBean.getUri());
                                    binding.videoView.start();
                                }
                            }
                        }).build(MainActivity.this);
            }
        });

    }
}
