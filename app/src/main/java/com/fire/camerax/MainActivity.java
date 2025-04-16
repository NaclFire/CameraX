package com.fire.camerax;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
                        Log.e("MainActivity", "onCameraResult: " + cameraResultBean.getPath());
                    }
                }).build(MainActivity.this);
            }
        });

    }
}
