package com.example.injehealth;

import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.DietItem;
import com.example.injehealth.db.entity.DietLog;
import com.example.injehealth.util.PhotoPickerHelper;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class AddMealSheet extends BottomSheetDialogFragment {

    private String date;
    private Calendar selectedTime;
    private TextView tvSelectedTime;
    private LinearLayout containerFoodItems;
    private PhotoPickerHelper photoPickerHelper;
    private Uri selectedPhotoUri = null;
    private ImageView ivPhotoPreview;
    private TextView btnPhotoRemove;

    public AddMealSheet() {
        photoPickerHelper = new PhotoPickerHelper(this);
    }

    public static AddMealSheet newInstance(String date) {
        AddMealSheet sheet = new AddMealSheet();
        Bundle args = new Bundle();
        args.putString("date", date);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_meal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        date = getArguments() != null ? getArguments().getString("date") : null;
        if (date == null) {
            date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        }

        selectedTime = Calendar.getInstance();
        tvSelectedTime = view.findViewById(R.id.tvSelectedTime);
        containerFoodItems = view.findViewById(R.id.containerFoodItems);
        ivPhotoPreview = view.findViewById(R.id.ivPhotoPreview);
        btnPhotoRemove = view.findViewById(R.id.btnPhotoRemove);

        // 회전 등 세션 내 상태 복원
        if (savedInstanceState != null) {
            String uriStr = savedInstanceState.getString("pending_photo_uri");
            if (uriStr != null) {
                selectedPhotoUri = Uri.parse(uriStr);
                Glide.with(requireContext()).load(selectedPhotoUri).centerCrop().into(ivPhotoPreview);
                ivPhotoPreview.setVisibility(View.VISIBLE);
                btnPhotoRemove.setVisibility(View.VISIBLE);
                view.findViewById(R.id.llPhotoHint).setVisibility(View.GONE);
            }
        }

        updateTimeDisplay();

        // 시간 변경 버튼
        view.findViewById(R.id.btnChangeTime).setOnClickListener(v -> {
            new TimePickerDialog(requireContext(),
                    (tp, hour, minute) -> {
                        selectedTime.set(Calendar.HOUR_OF_DAY, hour);
                        selectedTime.set(Calendar.MINUTE, minute);
                        updateTimeDisplay();
                    },
                    selectedTime.get(Calendar.HOUR_OF_DAY),
                    selectedTime.get(Calendar.MINUTE),
                    true
            ).show();
        });

        // 사진 버튼
        view.findViewById(R.id.btnPhotoCamera).setOnClickListener(v ->
            photoPickerHelper.launchCamera(this::onPhotoSelected)
        );
        view.findViewById(R.id.btnPhotoGallery).setOnClickListener(v ->
            photoPickerHelper.launchGallery(this::onPhotoSelected)
        );
        btnPhotoRemove.setOnClickListener(v -> {
            selectedPhotoUri = null;
            ivPhotoPreview.setVisibility(View.GONE);
            btnPhotoRemove.setVisibility(View.GONE);
            view.findViewById(R.id.llPhotoHint).setVisibility(View.VISIBLE);
        });

        // 음식 추가 버튼 — 첫 번째 음식 행 자동 추가
        addFoodRow();
        view.findViewById(R.id.btnAddFood).setOnClickListener(v -> addFoodRow());

        // 취소
        view.findViewById(R.id.ivClose).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());

        // 저장
        view.findViewById(R.id.btnSave).setOnClickListener(v -> saveMeal(view));
    }

    private void onPhotoSelected(Uri uri) {
        if (uri == null) return;
        selectedPhotoUri = uri;
        Glide.with(requireContext())
                .load(uri)
                .centerCrop()
                .into(ivPhotoPreview);
        ivPhotoPreview.setVisibility(View.VISIBLE);
        btnPhotoRemove.setVisibility(View.VISIBLE);
        if (getView() != null) {
            getView().findViewById(R.id.llPhotoHint).setVisibility(View.GONE);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedPhotoUri != null) {
            outState.putString("pending_photo_uri", selectedPhotoUri.toString());
        }
    }

    private void updateTimeDisplay() {
        String time = String.format(Locale.getDefault(), "%02d:%02d",
                selectedTime.get(Calendar.HOUR_OF_DAY),
                selectedTime.get(Calendar.MINUTE));
        tvSelectedTime.setText(time);
    }

    private void addFoodRow() {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_food_input, containerFoodItems, false);
        containerFoodItems.addView(row);
    }

    private void saveMeal(View rootView) {
        EditText etMemo = rootView.findViewById(R.id.etMemo);
        String memo = etMemo.getText().toString().trim();

        // 음식 행 수집
        List<DietItem> foodItems = new ArrayList<>();
        for (int i = 0; i < containerFoodItems.getChildCount(); i++) {
            View row = containerFoodItems.getChildAt(i);
            EditText etName   = row.findViewById(R.id.etFoodName);
            EditText etKcal   = row.findViewById(R.id.etKcal);
            EditText etCarbs  = row.findViewById(R.id.etCarbs);
            EditText etProt   = row.findViewById(R.id.etProtein);
            EditText etFat    = row.findViewById(R.id.etFat);

            String name = etName.getText().toString().trim();
            if (name.isEmpty()) continue; // 빈 행 스킵

            DietItem item = new DietItem();
            item.food_name = name;
            item.amount    = 0;
            item.unit      = "g";
            try { item.kcal    = Double.parseDouble(etKcal.getText().toString()); } catch (NumberFormatException e) { item.kcal = 0; /* intentional: default to 0 if input is empty or invalid */ }
            try { item.carbs   = Double.parseDouble(etCarbs.getText().toString()); } catch (NumberFormatException e) { item.carbs = 0; /* intentional: default to 0 if input is empty or invalid */ }
            try { item.protein = Double.parseDouble(etProt.getText().toString()); } catch (NumberFormatException e) { item.protein = 0; /* intentional: default to 0 if input is empty or invalid */ }
            try { item.fat     = Double.parseDouble(etFat.getText().toString()); } catch (NumberFormatException e) { item.fat = 0; /* intentional: default to 0 if input is empty or invalid */ }
            foodItems.add(item);
        }

        if (foodItems.isEmpty()) {
            Toast.makeText(requireContext(), R.string.diet_add_error_no_food, Toast.LENGTH_SHORT).show();
            return;
        }

        // eaten_at 조합
        String eatenAt = date + " " + String.format(Locale.getDefault(), "%02d:%02d",
                selectedTime.get(Calendar.HOUR_OF_DAY),
                selectedTime.get(Calendar.MINUTE));

        DietLog log = new DietLog();
        log.eatenAt   = eatenAt;
        log.memo      = memo.isEmpty() ? null : memo;
        log.photoPath = selectedPhotoUri != null ? selectedPhotoUri.toString() : null;

        // DB 저장 (트랜잭션)
        AppDatabase db = AppDatabase.getInstance(requireContext());
        Executors.newSingleThreadExecutor().execute(() -> {
            db.runInTransaction(() -> {
                long logId = db.dietLogDao().insert(log);
                for (DietItem item : foodItems) {
                    item.log_id = (int) logId;
                    db.dietItemDao().insert(item);
                }
            });
            requireActivity().runOnUiThread(() -> {
                if (getActivity() instanceof DietDayActivity) {
                    ((DietDayActivity) getActivity()).loadData();
                }
                dismiss();
            });
        });
    }
}
