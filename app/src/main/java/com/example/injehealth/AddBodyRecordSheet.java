package com.example.injehealth;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.BodyRecord;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class AddBodyRecordSheet extends BottomSheetDialogFragment {

    private Calendar selectedTime;
    private TextView tvSelectedTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_body_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        selectedTime = Calendar.getInstance();
        tvSelectedTime = view.findViewById(R.id.tvSelectedTime);
        updateTimeDisplay();

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

        view.findViewById(R.id.ivClose).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnSave).setOnClickListener(v -> saveRecord(view));
    }

    private void updateTimeDisplay() {
        tvSelectedTime.setText(String.format(Locale.getDefault(), "%02d:%02d",
                selectedTime.get(Calendar.HOUR_OF_DAY),
                selectedTime.get(Calendar.MINUTE)));
    }

    private void saveRecord(View rootView) {
        EditText etWeight   = rootView.findViewById(R.id.etWeight);
        EditText etMuscle   = rootView.findViewById(R.id.etMuscle);
        EditText etFatMass  = rootView.findViewById(R.id.etFatMass);
        EditText etFatRate  = rootView.findViewById(R.id.etFatRate);

        String weightStr = etWeight.getText().toString().trim();
        if (weightStr.isEmpty()) {
            Toast.makeText(requireContext(), "체중을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String recordedAt = date + " " + String.format(Locale.getDefault(), "%02d:%02d",
                selectedTime.get(Calendar.HOUR_OF_DAY),
                selectedTime.get(Calendar.MINUTE));

        BodyRecord record = new BodyRecord();
        record.recorded_at = recordedAt;
        record.weight = parseDouble(weightStr);
        record.muscle_mass = parseDouble(etMuscle.getText().toString().trim());
        record.body_fat_mass = parseDouble(etFatMass.getText().toString().trim());
        record.body_fat_rate = parseDouble(etFatRate.getText().toString().trim());

        AppDatabase db = AppDatabase.getInstance(requireContext());
        Executors.newSingleThreadExecutor().execute(() -> {
            db.bodyRecordDao().insert(record);
            requireActivity().runOnUiThread(() -> {
                if (getParentFragment() instanceof MyinbodyFragment) {
                    ((MyinbodyFragment) getParentFragment()).loadRecords();
                }
                dismiss();
            });
        });
    }

    private double parseDouble(String text) {
        if (text == null || text.isEmpty()) return 0;
        try { return Double.parseDouble(text); } catch (NumberFormatException e) { return 0; }
    }
}
