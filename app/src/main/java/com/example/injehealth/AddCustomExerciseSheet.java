package com.example.injehealth;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.Exercise;
import com.example.injehealth.util.BodyPartLabels;
import com.example.injehealth.util.PhotoFileHelper;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

public class AddCustomExerciseSheet extends BottomSheetDialogFragment {

    private Uri selectedImageUri = null;
    private String selectedBodyPart = null;

    private ImageView ivPreview;
    private LinearLayout llUploadHint;
    private EditText etExerciseName;
    private Button btnAdd;

    // 부위 버튼 목록
    private final List<TextView> bodyPartBtns = new ArrayList<>();
    private final String[] bodyPartKeys = {"chest", "back", "legs", "shoulders", "arms", "cardio"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_custom_exercise, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivPreview     = view.findViewById(R.id.ivPreview);
        llUploadHint  = view.findViewById(R.id.llUploadHint);
        etExerciseName = view.findViewById(R.id.etExerciseName);
        btnAdd        = view.findViewById(R.id.btnAdd);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        ImageView ivClose = view.findViewById(R.id.ivClose);
        View flImageUpload = view.findViewById(R.id.flImageUpload);

        // 닫기
        ivClose.setOnClickListener(v -> dismiss());
        btnCancel.setOnClickListener(v -> dismiss());

        // 이미지 업로드 영역 클릭 → 부모 Fragment 통해 갤러리 실행
        flImageUpload.setOnClickListener(v -> launchParentGallery());

        // 부위 버튼 설정
        int[] btnIds = {R.id.btnChest, R.id.btnBack, R.id.btnLegs,
                        R.id.btnShoulders, R.id.btnArms, R.id.btnCardio};
        for (int i = 0; i < btnIds.length; i++) {
            TextView btn = view.findViewById(btnIds[i]);
            bodyPartBtns.add(btn);
            final int idx = i;
            btn.setOnClickListener(v -> selectBodyPart(idx));
        }

        // 추가하기 버튼
        btnAdd.setOnClickListener(v -> onAddClicked());
    }

    /** 부모 ExerciseCatalogFragment에 갤러리 실행 위임 */
    private void launchParentGallery() {
        if (getParentFragment() instanceof ExerciseCatalogFragment) {
            ((ExerciseCatalogFragment) getParentFragment()).launchGalleryFor(uri -> {
                if (uri == null) return;
                selectedImageUri = uri;
                // 미리보기 표시
                llUploadHint.setVisibility(View.GONE);
                ivPreview.setVisibility(View.VISIBLE);
                Glide.with(this).load(uri).centerCrop().into(ivPreview);
            });
        } else {
            Toast.makeText(requireContext(), "갤러리를 열 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    /** 부위 선택 상태 갱신 */
    private void selectBodyPart(int idx) {
        selectedBodyPart = bodyPartKeys[idx];
        for (int i = 0; i < bodyPartBtns.size(); i++) {
            bodyPartBtns.get(i).setBackgroundResource(i == idx
                    ? R.drawable.bg_segment_selected
                    : R.drawable.bg_segment_unselected);
        }
    }

    /** 추가하기 클릭 처리 */
    private void onAddClicked() {
        String name = etExerciseName.getText().toString().trim();
        if (name.isEmpty()) {
            etExerciseName.setError("운동 이름을 입력하세요");
            return;
        }
        if (selectedBodyPart == null) {
            Toast.makeText(requireContext(), "부위를 선택하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        Context ctx = requireContext().getApplicationContext();
        Uri imgUri = selectedImageUri;
        String bodyPart = selectedBodyPart;

        btnAdd.setEnabled(false);

        Executors.newSingleThreadExecutor().execute(() -> {
            String imageType;
            String imageRef;

            if (imgUri != null) {
                // 갤러리 사진 → getFilesDir()/exercise_photos/<uuid>.jpg 복사
                String fileName = UUID.randomUUID().toString() + ".jpg";
                try {
                    imageRef = PhotoFileHelper.copyToSubdir(ctx, imgUri, "exercise_photos", fileName);
                    imageType = "file";
                } catch (IOException e) {
                    // 복사 실패 시 이모지 fallback
                    imageRef = BodyPartLabels.emoji(bodyPart);
                    imageType = "emoji";
                }
            } else {
                imageType = "emoji";
                imageRef  = BodyPartLabels.emoji(bodyPart);
            }

            Exercise ex = new Exercise();
            ex.name       = name;
            ex.body_part  = bodyPart;
            ex.image_type = imageType;
            ex.image_ref  = imageRef;
            ex.is_custom  = 1;
            ex.description = null;

            AppDatabase.getInstance(ctx).exerciseDao().insert(ex);

            // UI 스레드: 시트 닫기 + 부모 새로고침
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (getParentFragment() instanceof ExerciseCatalogFragment) {
                        ((ExerciseCatalogFragment) getParentFragment()).onCustomExerciseAdded();
                    }
                    dismiss();
                });
            }
        });
    }
}
