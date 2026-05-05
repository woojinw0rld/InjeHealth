package com.example.injehealth.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.injehealth.R;
import com.example.injehealth.db.AppDatabase;
import com.example.injehealth.db.entity.DietItem;
import com.example.injehealth.db.entity.DietLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.ViewHolder> {

    private static final java.util.concurrent.ExecutorService EXECUTOR =
            java.util.concurrent.Executors.newSingleThreadExecutor();

    public interface OnMealActionListener {
        void onMoreClick(DietLog log, View anchor);
        void onAddPhotoClick(DietLog log);
        void onPhotoClick(DietLog log);
    }

    private List<DietLog> items = new ArrayList<>();
    private OnMealActionListener listener;

    public void setItems(List<DietLog> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnMealActionListener(OnMealActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DietLog log = items.get(position);
        Context context = holder.itemView.getContext();

        // 시간 표시: eaten_at "yyyy-MM-dd HH:mm" → "HH:mm"
        if (log.eatenAt != null && log.eatenAt.length() >= 16) {
            holder.tvMealTime.setText(log.eatenAt.substring(11, 16));
        } else {
            holder.tvMealTime.setText("--:--");
        }

        // 메모
        if (log.memo != null && !log.memo.isEmpty()) {
            holder.tvMealMemo.setText(log.memo);
            holder.tvMealMemo.setVisibility(View.VISIBLE);
        } else {
            holder.tvMealMemo.setVisibility(View.GONE);
        }

        // 사진
        if (log.photoPath != null) {
            Glide.with(context)
                    .load(Uri.parse(log.photoPath))
                    .centerCrop()
                    .placeholder(R.color.surface_variant)
                    .into(holder.ivMealPhoto);
            holder.ivMealPhoto.setVisibility(View.VISIBLE);
            holder.btnAddMealPhoto.setVisibility(View.GONE);
            holder.ivMealPhoto.setOnClickListener(v -> {
                if (listener != null) listener.onPhotoClick(log);
            });
        } else {
            holder.ivMealPhoto.setVisibility(View.GONE);
            holder.btnAddMealPhoto.setVisibility(View.VISIBLE);
            holder.btnAddMealPhoto.setOnClickListener(v -> {
                if (listener != null) listener.onAddPhotoClick(log);
            });
        }

        // more 버튼
        holder.ivMealMore.setOnClickListener(v -> {
            if (listener != null) listener.onMoreClick(log, v);
        });

        // DietItem 합산 (백그라운드 스레드)
        // position을 미리 캡처해 stale ViewHolder 방지
        final int boundPosition = position;
        EXECUTOR.execute(() -> {
            List<DietItem> dietItems = AppDatabase.getInstance(context)
                    .dietItemDao().getByLogId(log.id);
            float kcal = 0, carbs = 0, protein = 0, fat = 0;
            for (DietItem item : dietItems) {
                kcal    += item.kcal;
                carbs   += item.carbs;
                protein += item.protein;
                fat     += item.fat;
            }
            final float fKcal = kcal, fCarbs = carbs, fProtein = protein, fFat = fat;
            // UI 업데이트는 메인 스레드에서 — ViewHolder가 재사용됐으면 스킵
            holder.itemView.post(() -> {
                if (holder.getAdapterPosition() != boundPosition) return;
                Context ctx = holder.itemView.getContext();
                holder.tvMealKcal.setText(ctx.getString(R.string.diet_kcal_format, fKcal));
                holder.tvMealCarbs.setText(ctx.getString(R.string.diet_macro_carbs_format, fCarbs));
                holder.tvMealProtein.setText(ctx.getString(R.string.diet_macro_protein_format, fProtein));
                holder.tvMealFat.setText(ctx.getString(R.string.diet_macro_fat_format, fFat));
            });
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvMealTime;
        final TextView tvMealKcal;
        final TextView tvMealCarbs;
        final TextView tvMealProtein;
        final TextView tvMealFat;
        final TextView tvMealMemo;
        final ImageView ivMealPhoto;
        final ImageView ivMealMore;
        final TextView btnAddMealPhoto;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMealTime      = itemView.findViewById(R.id.tvMealTime);
            tvMealKcal      = itemView.findViewById(R.id.tvMealKcal);
            tvMealCarbs     = itemView.findViewById(R.id.tvMealCarbs);
            tvMealProtein   = itemView.findViewById(R.id.tvMealProtein);
            tvMealFat       = itemView.findViewById(R.id.tvMealFat);
            tvMealMemo      = itemView.findViewById(R.id.tvMealMemo);
            ivMealPhoto     = itemView.findViewById(R.id.ivMealPhoto);
            ivMealMore      = itemView.findViewById(R.id.ivMealMore);
            btnAddMealPhoto = itemView.findViewById(R.id.btnAddMealPhoto);
        }
    }
}
