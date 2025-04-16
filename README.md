# CameraX

使用方法：

```java
new CameraXActivity.Builder()
        .setMaxTime(30)// 最长录制时间，默认30秒
        .setMinTime(2)// 最短录制时间，可选，默认0
        .setSaveFolder("MyApp")// 保存文件夹名，位于DCIM目录下，。
        .setOnCameraCallback(new CameraXActivity.OnCameraCallback() {
    @Override
    public void onCameraResult(CameraResultBean cameraResultBean) {
      	// cameraResultBean结构：
      	// path：文件绝对路径
      	// type：类型：CAMERA_RESULT_TYPE_PHOTO、CAMERA_RESULT_TYPE_VIDEO
      	// uri：媒体文件Uri
        Log.e("MainActivity", "onCameraResult: " + cameraResultBean.getPath());
    }
}).build(MainActivity.this);
```