package com.example.injehealth;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.injehealth.adapter.MealAdapter;
import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.DietItem;
import com.example.injehealth.db.entity.DietLog;
import com.example.injehealth.util.PhotoFileHelper;
import com.example.injehealth.util.SystemBarHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class DietDayActivity extends AppCompatActivity {

    private String date;
    private MealAdapter adapter;
    private View mealEmptyState;
    private RecyclerView recyclerMeals;
    private TextView tvDayTotalKcal, tvDayCarbs, tvDayProtein, tvDayFat;

    private DietLog pendingPhotoLog = null;
    private ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher;
    private ActivityResultLauncher<String> galleryFallbackLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diet_day);

        SystemBarHelper.applyPadding(this, R.id.main);

        // 갤러리 런처 등록 (API 33+)
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> { if (uri != null) onPhotoSelected(uri); }
        );
        // 갤러리 런처 등록 (API 26~32 fallback)
        galleryFallbackLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) onPhotoSelected(uri); }
        );

        date = getIntent().getStringExtra("date");
        if (date == null) {
            date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // 날짜 타이틀 포맷
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN).parse(date);
            toolbar.setTitle(new SimpleDateFormat(getString(R.string.diet_day_title_date_format), Locale.KOREAN).format(d));
        } catch (ParseException e) {
            toolbar.setTitle(date);
        }

        tvDayTotalKcal = findViewById(R.id.tvDayTotalKcal);
        tvDayCarbs     = findViewById(R.id.tvDayCarbs);
        tvDayProtein   = findViewById(R.id.tvDayProtein);
        tvDayFat       = findViewById(R.id.tvDayFat);
        recyclerMeals  = findViewById(R.id.recyclerMeals);
        mealEmptyState = findViewById(R.id.mealEmptyState);

        adapter = new MealAdapter();
        recyclerMeals.setLayoutManager(new LinearLayoutManager(this));
        recyclerMeals.setAdapter(adapter);

        adapter.setOnMealActionListener(new MealAdapter.OnMealActionListener() {
            @Override
            public void onMoreClick(DietLog log, View anchor) {
                showMealMoreMenu(log, anchor);
            }

            @Override
            public void onAddPhotoClick(DietLog log) {
                pendingPhotoLog = log;
                launchGallery();
            }

            @Override
            public void onPhotoClick(DietLog log) {
                showPhotoMenu(log);
            }
        });

        FloatingActionButton fabAddMeal = findViewById(R.id.fabAddMeal);
        fabAddMeal.setOnClickListener(v -> {
            AddMealSheet sheet = AddMealSheet.newInstance(date);
            sheet.show(getSupportFragmentManager(), "add_meal");
        });

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void launchGallery() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            galleryLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        } else {
            galleryFallbackLauncher.launch("image/*");
        }
    }

    private void onPhotoSelected(Uri uri) {
        if (uri == null || pendingPhotoLog == null) return;
        DietLog log = pendingPhotoLog;
        pendingPhotoLog = null;
        String oldPath = log.photoPath;
        log.photoPath = uri.toString();
        Executors.newSingleThreadExecutor().execute(() -> {
            if (oldPath != null) PhotoFileHelper.deleteIfLocal(this, oldPath);
            AppDatabase.getInstance(this).dietLogDao().update(log);
            runOnUiThread(this::loadData);
        });
    }

    private void showMealMoreMenu(DietLog log, View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, getString(R.string.diet_meal_edit_time));
        popup.getMenu().add(0, 2, 0, getString(R.string.diet_meal_edit_memo));
        popup.getMenu().add(0, 3, 0, getString(R.string.diet_meal_delete));
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) { showTimeEditDialog(log); return true; }
            if (id == 2) { showMemoEditDialog(log); return true; }
            if (id == 3) { showMealDeleteConfirm(log); return true; }
            return false;
        });
        popup.show();
    }

    private void showTimeEditDialog(DietLog log) {
        int hour = 12, minute = 0;
        if (log.eatenAt != null && log.eatenAt.length() >= 16) {
            try {
                hour   = Integer.parseInt(log.eatenAt.substring(11, 13));
                minute = Integer.parseInt(log.eatenAt.substring(14, 16));
            } catch (NumberFormatException ignored) { /* intentional: default to 12:00 if time format is unexpected */ }
        }
        new TimePickerDialog(this, (view, h, m) -> {
            String newTime = String.format(Locale.getDefault(), "%02d:%02d", h, m);
            String datePrefix = (log.eatenAt != null && log.eatenAt.length() >= 10)
                    ? log.eatenAt.substring(0, 10)
                    : date;
            log.eatenAt = datePrefix + " " + newTime;
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase.getInstance(this).dietLogDao().update(log);
                runOnUiThread(this::loadData);
            });
        }, hour, minute, true).show();
    }

    private void showMemoEditDialog(DietLog log) {
        EditText editText = new EditText(this);
        editText.setText(log.memo != null ? log.memo : "");
        new AlertDialog.Builder(this)
                .setTitle(R.string.diet_meal_edit_memo)
                .setView(editText)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    log.memo = editText.getText().toString().trim();
                    Executors.newSingleThreadExecutor().execute(() -> {
                        AppDatabase.getInstance(this).dietLogDao().update(log);
                        runOnUiThread(this::loadData);
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showMealDeleteConfirm(DietLog log) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.diet_meal_delete_confirm)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        if (log.photoPath != null) PhotoFileHelper.deleteIfLocal(this, log.photoPath);
                        AppDatabase.getInstance(this).dietLogDao().delete(log);
                        runOnUiThread(this::loadData);
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showPhotoMenu(DietLog log) {
        new AlertDialog.Builder(this)
                .setItems(new String[]{
                        getString(R.string.diet_photo_change),
                        getString(R.string.diet_photo_delete)
                }, (d, which) -> {
                    if (which == 0) {
                        pendingPhotoLog = log;
                        launchGallery();
                    } else {
                        showPhotoDeleteConfirm(log);
                    }
                })
                .show();
    }

    private void showPhotoDeleteConfirm(DietLog log) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.diet_photo_delete_confirm)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        if (log.photoPath != null) PhotoFileHelper.deleteIfLocal(this, log.photoPath);
                        log.photoPath = null;
                        AppDatabase.getInstance(this).dietLogDao().update(log);
                        runOnUiThread(this::loadData);
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void loadData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<DietLog> logs = db.dietLogDao().getByDate(date);

            double totalKcal = 0, totalCarbs = 0, totalProtein = 0, totalFat = 0;
            for (DietLog log : logs) {
                List<DietItem> items = db.dietItemDao().getByLogId(log.id);
                for (DietItem item : items) {
                    totalKcal += item.kcal;
                    totalCarbs += item.carbs;
                    totalProtein += item.protein;
                    totalFat += item.fat;
                }
            }
            final double fKcal = totalKcal, fCarbs = totalCarbs, fProtein = totalProtein, fFat = totalFat;

            runOnUiThread(() -> {
                adapter.setItems(logs);
                tvDayTotalKcal.setText(getString(R.string.diet_kcal_format, fKcal));
                tvDayCarbs.setText(getString(R.string.diet_macro_carbs_format, fCarbs));
                tvDayProtein.setText(getString(R.string.diet_macro_protein_format, fProtein));
                tvDayFat.setText(getString(R.string.diet_macro_fat_format, fFat));
                updateEmptyState(logs);
            });
        });
    }

    private void updateEmptyState(List<DietLog> logs) {
        if (logs == null || logs.isEmpty()) {
            recyclerMeals.setVisibility(View.GONE);
            mealEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerMeals.setVisibility(View.VISIBLE);
            mealEmptyState.setVisibility(View.GONE);
        }
    }
}
