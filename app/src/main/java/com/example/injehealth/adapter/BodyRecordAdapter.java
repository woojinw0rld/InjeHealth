package com.example.injehealth.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.injehealth.R;
import com.example.injehealth.db.entity.BodyRecord;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Myinbody 기록 내역 RecyclerView 어댑터
 *
 * - BodyRecord를 item_body_record 레이아웃에 바인딩
 * - 조회 모드: 날짜 + 체중(크게) + 부가 수치(근육/체지방/체지방률) + 수정/삭제 버튼
 * - 수정 모드: 4개 필드 인라인 편집 + 저장/취소 버튼
 */
public class BodyRecordAdapter extends RecyclerView.Adapter<BodyRecordAdapter.RecordViewHolder> {

    /** 수정/삭제 콜백 인터페이스 */
    public interface OnRecordActionListener {
        void onUpdate(BodyRecord record);
        void onDelete(BodyRecord record);
    }

    private final List<BodyRecord> items = new ArrayList<>();
    private final OnRecordActionListener listener;

    // 날짜 포맷: "yyyy-MM-dd" → "yyyy년 M월 d일"
    private final SimpleDateFormat dateFormatIn = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat dateFormatOut = new SimpleDateFormat("yyyy년 M월 d일", Locale.getDefault());

    public BodyRecordAdapter(OnRecordActionListener listener) {
        this.listener = listener;
    }

    /**
     * 데이터 전체 교체 후 갱신
     * @param newItems 새 기록 리스트 (null 허용)
     */
    public void setItems(List<BodyRecord> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_body_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        BodyRecord record = items.get(position);
        holder.bind(record, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * 기록 아이템 ViewHolder
     *
     * 두 가지 모드:
     * - layout_view (조회): 날짜, 체중, 부가 수치, 수정/삭제 버튼
     * - layout_edit (수정): 4개 입력 필드 + 저장/취소 버튼
     */
    class RecordViewHolder extends RecyclerView.ViewHolder {

        // 조회 모드 뷰
        private final LinearLayout layoutView;
        private final TextView tvDate, tvWeight, tvSubValues;
        private final ImageView btnEdit, btnDelete;

        // 수정 모드 뷰
        private final LinearLayout layoutEdit;
        private final TextView tvEditDate;
        private final EditText etEditWeight, etEditMuscle, etEditFatMass, etEditFatRate;
        private final ImageView btnSave, btnCancel;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);

            // 조회 모드
            layoutView = itemView.findViewById(R.id.layout_view);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvWeight = itemView.findViewById(R.id.tv_weight);
            tvSubValues = itemView.findViewById(R.id.tv_sub_values);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);

            // 수정 모드
            layoutEdit = itemView.findViewById(R.id.layout_edit);
            tvEditDate = itemView.findViewById(R.id.tv_edit_date);
            etEditWeight = itemView.findViewById(R.id.et_edit_weight);
            etEditMuscle = itemView.findViewById(R.id.et_edit_muscle);
            etEditFatMass = itemView.findViewById(R.id.et_edit_fat_mass);
            etEditFatRate = itemView.findViewById(R.id.et_edit_fat_rate);
            btnSave = itemView.findViewById(R.id.btn_save);
            btnCancel = itemView.findViewById(R.id.btn_cancel);
        }

        public void bind(BodyRecord record, OnRecordActionListener listener) {
            Context context = itemView.getContext();

            // 기본: 조회 모드 표시
            layoutView.setVisibility(View.VISIBLE);
            layoutEdit.setVisibility(View.GONE);

            // 날짜 포맷
            String formattedDate = formatDate(record.date);
            tvDate.setText(formattedDate);
            tvEditDate.setText(formattedDate);

            // 체중 (크게 표시)
            tvWeight.setText(String.format(Locale.getDefault(), "%.1fkg", record.weight));

            // 부가 수치: "근육 32.5kg · 체지방 15.2kg · 체지방률 20.3%"
            String sub = String.format(Locale.getDefault(),
                    context.getString(R.string.myinbody_sub_format),
                    formatValue(record.muscle_mass),
                    formatValue(record.body_fat_mass),
                    formatValue(record.body_fat_rate));
            tvSubValues.setText(sub);

            // ── 수정 버튼 → 수정 모드 전환 ──
            btnEdit.setOnClickListener(v -> {
                layoutView.setVisibility(View.GONE);
                layoutEdit.setVisibility(View.VISIBLE);

                // 기존 값을 EditText에 채움
                etEditWeight.setText(formatValue(record.weight));
                etEditMuscle.setText(formatValue(record.muscle_mass));
                etEditFatMass.setText(formatValue(record.body_fat_mass));
                etEditFatRate.setText(formatValue(record.body_fat_rate));
            });

            // ── 삭제 버튼 ──
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(record);
                }
            });

            // ── 저장 버튼 → DB 업데이트 콜백 ──
            btnSave.setOnClickListener(v -> {
                if (listener != null) {
                    // EditText에서 값 추출하여 record에 반영
                    record.weight = parseDouble(etEditWeight.getText().toString());
                    record.muscle_mass = parseDouble(etEditMuscle.getText().toString());
                    record.body_fat_mass = parseDouble(etEditFatMass.getText().toString());
                    record.body_fat_rate = parseDouble(etEditFatRate.getText().toString());
                    listener.onUpdate(record);
                }
            });

            // ── 취소 버튼 → 조회 모드 복귀 ──
            btnCancel.setOnClickListener(v -> {
                layoutView.setVisibility(View.VISIBLE);
                layoutEdit.setVisibility(View.GONE);
            });
        }

        /**
         * "yyyy-MM-dd" → "yyyy년 M월 d일" 변환
         */
        private String formatDate(String dateStr) {
            if (dateStr == null) return "";
            try {
                Date date = dateFormatIn.parse(dateStr);
                if (date != null) return dateFormatOut.format(date);
            } catch (ParseException e) {
                // 파싱 실패 시 원본 반환
            }
            return dateStr;
        }

        /**
         * double → 소수점 1자리 문자열 (예: 75.0 → "75.0")
         * 0이면 "0" 반환
         */
        private String formatValue(double value) {
            if (value == 0) return "0";
            return String.format(Locale.getDefault(), "%.1f", value);
        }

        /**
         * 문자열 → double 파싱 (빈 문자열이면 0 반환)
         */
        private double parseDouble(String text) {
            if (text == null || text.trim().isEmpty()) return 0;
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}
