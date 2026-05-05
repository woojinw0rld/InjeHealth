package com.example.injehealth.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * 갤러리 선택 + 카메라 촬영 통합 헬퍼.
 * Fragment 생성자에서 초기화하여 ActivityResultLauncher를 등록한다.
 */
public class PhotoPickerHelper {

    private final Fragment fragment;

    // 갤러리 런처 (API 33+: PickVisualMedia, 미만: ACTION_GET_CONTENT)
    private ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;
    private ActivityResultLauncher<String> getContentLauncher;

    // 카메라 런처
    private ActivityResultLauncher<Uri> cameraLauncher;

    // 현재 촬영 중인 파일 Uri (카메라 결과 수신 시 사용)
    private Uri pendingCameraUri;

    // 현재 콜백
    private Consumer<Uri> pendingCallback;

    public PhotoPickerHelper(Fragment fragment) {
        this.fragment = fragment;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: PickVisualMedia
            pickMediaLauncher = fragment.registerForActivityResult(
                    new ActivityResultContracts.PickVisualMedia(),
                    uri -> {
                        Consumer<Uri> cb = pendingCallback;
                        pendingCallback = null;
                        if (cb != null) cb.accept(uri);
                    }
            );
        } else {
            // API 26~32: ACTION_GET_CONTENT fallback
            getContentLauncher = fragment.registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        Consumer<Uri> cb = pendingCallback;
                        pendingCallback = null;
                        if (cb != null) cb.accept(uri);
                    }
            );
        }

        // 카메라 런처
        cameraLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    Consumer<Uri> cb = pendingCallback;
                    Uri capturedUri = pendingCameraUri;
                    pendingCallback = null;
                    pendingCameraUri = null;
                    if (cb != null) {
                        cb.accept(Boolean.TRUE.equals(success) ? capturedUri : null);
                    }
                }
        );
    }

    /**
     * 갤러리를 열어 이미지를 선택한다.
     * 결과 Uri가 null이면 callback.accept(null) 호출.
     */
    public void launchGallery(Consumer<Uri> callback) {
        pendingCallback = callback;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        } else {
            getContentLauncher.launch("image/*");
        }
    }

    /**
     * 카메라를 열어 사진을 촬영한다.
     * 촬영 성공 시 저장된 파일의 content Uri를 callback으로 전달.
     * IOException 또는 촬영 취소 시 callback.accept(null).
     */
    public void launchCamera(Consumer<Uri> callback) {
        try {
            File photoFile = PhotoFileHelper.createDietPhotoFile(fragment.requireContext());
            Uri contentUri = PhotoFileHelper.toContentUri(fragment.requireContext(), photoFile);
            pendingCameraUri = contentUri;
            pendingCallback = callback;
            cameraLauncher.launch(contentUri);
        } catch (IOException e) {
            pendingCameraUri = null;
            pendingCallback = null;
            callback.accept(null);
        }
    }
}
