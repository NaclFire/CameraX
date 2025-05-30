package com.fire.camera.bean;

import android.net.Uri;

import java.io.Serializable;

public class CameraResultBean implements Serializable {
    public static final int CAMERA_RESULT_TYPE_PHOTO = 1;
    public static final int CAMERA_RESULT_TYPE_VIDEO = 2;
    private String path;
    private int type;
    private Uri uri;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }
}
