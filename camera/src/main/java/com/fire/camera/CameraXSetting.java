package com.fire.camera;

public class CameraXSetting {
    public static final int CAMERA_ORIENTATION_VERTICAL = 0x01;
    public static final int CAMERA_ORIENTATION_HORIZONTAL = 0x02;
    public static final int CAMERA_MODE_PHOTO = 0x11;
    public static final int CAMERA_MODE_VIDEO = 0x12;
    /**
     * 保存到DCIM下的文件夹名称
     */
    public static String CAMERA_SAVE_FOLDER = "";
    /**
     * 最长录制时间，单位秒
     */
    public static int CAMERA_VIDEO_MAX_TIME = 30;
    /**
     * 最短录制时间，单位秒
     */
    public static int CAMERA_VIDEO_MIN_TIME = 0;


}
