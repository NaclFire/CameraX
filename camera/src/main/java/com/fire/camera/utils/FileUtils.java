package com.fire.camera.utils;

import android.os.Build;
import android.util.Base64;

import androidx.camera.core.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FileUtils {
    private static final String TAG = "FileUtils";

    /**
     * 移动文件或文件夹
     *
     * @param source    可以是文件也可以是文件夹
     * @param target    必须是文件夹
     * @param existJump 目标文件已经存在时，true表示跳过，false表示文件重命名（文件名加数字递增的方式）移动
     * @return 返回所有移动成功后的目标文件路径
     */
    public static List<String> moveData(File source, File target, boolean existJump, boolean isMove) {
        if (source == null || target == null) {
            return null;
        }

        if (!target.exists()) {
            target.mkdirs();
        } else {
            if (!target.isDirectory()) {
                throw new IllegalArgumentException("target must is directory!!!");
            }
        }

        String sourceS = source.getPath();
        String targetS = target.getPath();
        String[] paths;
        if (source.isDirectory()) {
            paths = source.list();
        } else {
            sourceS = source.getParent();
            paths = new String[]{source.getName()};
        }

        List<String> successList = new ArrayList<>();

        for (String tmp : paths) {
            File tmpFile = new File(sourceS + File.separator + tmp);
            File newFile = new File(targetS + File.separator + tmp);
            if (tmpFile.isDirectory()) {
                List<String> middleList = moveData(tmpFile, newFile, existJump, isMove);
                if (!middleList.isEmpty())
                    successList.addAll(middleList);
            } else {
                if (newFile.exists()) {
                    //不跳过
                    if (!existJump) {
                        // 递增文件名
                        for (int i = 1; ; i++) {
                            String[] arr = tmp.split("\\.");
                            String tmp2 = arr[0] + "(" + i + ")";
                            if (arr.length > 1) {
                                tmp2 += "." + arr[1];
                            }
                            newFile = new File(target, tmp2);
                            if (!newFile.exists())
                                break;
                        }

                        String successPath = moveFileCompat(tmpFile, newFile, isMove);
                        if (successPath != null && successPath.length() != 0) {
                            successList.add(successPath);
                        }
                    }
                } else {
                    String successPath = moveFileCompat(tmpFile, newFile, isMove);
                    if (successPath != null && successPath.length() != 0) {
                        successList.add(successPath);
                    }
                }
            }
        }
        if (source.isDirectory()
                && (source.list() == null || Objects.requireNonNull(source.list()).length == 0)) {
            boolean delete = source.delete();
            if (delete) {
                Logger.e("CameraXActivity", "删除成功");
            }
        }
        return successList;
    }

    private static String moveFileCompat(File oldFile, File newFile, boolean isCut) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                if (isCut) {
                    Files.move(oldFile.toPath(), newFile.toPath());
                } else {
                    Files.copy(oldFile.toPath(), newFile.toPath());
                }
                return newFile.getPath();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            boolean isCopySuccess = oldFile.renameTo(newFile);
            if (isCopySuccess) {
                return newFile.getPath();
            }
        }
        return null;
    }

    public static final int BUFFER_SIZE = 3 * 1024 * 1024; // 每次读取3MB

    public interface Base64ChunkHandler {
        void handleBase64Chunk(String base64Chunk, int current, int total);
    }

    public static void encodeFileToBase64InChunks(String filePath, Base64ChunkHandler chunkHandler) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int currentChunk = 0;
                File file = new File(filePath);
                int totalChunks = (int) Math.ceil((double) file.length() / BUFFER_SIZE);
                InputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(file);
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        // 将读取到的字节数组部分编码为Base64
                        String base64Chunk = Base64.encodeToString(buffer, 0, bytesRead, Base64.NO_WRAP);
                        currentChunk++;
                        // 调用处理程序处理Base64数据块
                        chunkHandler.handleBase64Chunk(base64Chunk, currentChunk, totalChunks);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            // 忽略关闭异常
                        }
                    }
                }
            }
        }).start();

    }

    private String encodeFileToBase64Binary(File file) {
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try (
                FileInputStream fileInputStream = new FileInputStream(file);
                BufferedInputStream buf = new BufferedInputStream(fileInputStream)) {
            int read = buf.read(bytes, 0, bytes.length);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String encoded = Base64.encodeToString(bytes, Base64.NO_WRAP);
        return encoded;
    }
}
