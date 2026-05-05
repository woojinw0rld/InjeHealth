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
import com.example.injehealth.db.model.DietDaySummary;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DietDayAdapter extends RecyclerView.Adapter<DietDayAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(String date);
    }

    private List<DietDaySummary> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public DietDayAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<DietDaySummary> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_diet_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DietDaySummary summary = items.get(position);
        Context context = holder.itemView.getContext();

        // 날짜 포맷 변환 "yyyy-MM-dd" → "M월 d일 E요일"
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN).parse(summary.date);
            holder.tvDate.setText(new SimpleDateFormat("M월 d일 E요일", Locale.KOREAN).format(d));
        } catch (ParseException e) {
            holder.tvDate.setText(summary.date);
        }

        holder.tvTotalKcal.setText(context.getString(R.string.diet_kcal_format, summary.totalKcal));
        holder.tvMealCount.setText(String.format(context.getString(R.string.diet_day_meal_count_format), summary.mealCount));
        holder.tvCarbs.setText(context.getString(R.string.diet_macro_carbs_format, summary.totalCarbs));
        holder.tvProtein.setText(context.getString(R.string.diet_macro_protein_format, summary.totalProtein));
        holder.tvFat.setText(context.getString(R.string.diet_macro_fat_format, summary.totalFat));

        if (summary.thumbnailPath != null) {
            Glide.with(context)
                    .load(Uri.parse(summary.thumbnailPath))
                    .centerCrop()
                    .placeholder(R.color.surface_variant)
                    .into(holder.ivThumbnail);
        } else {
            Glide.with(context)
                    .load((Object) null)
                    .placeholder(R.color.surface_variant)
                    .into(holder.ivThumbnail);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(summary.date));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDate;
        final TextView tvTotalKcal;
        final TextView tvMealCount;
        final TextView tvCarbs;
        final TextView tvProtein;
        final TextView tvFat;
        final ImageView ivThumbnail;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTotalKcal = itemView.findViewById(R.id.tvTotalKcal);
            tvMealCount = itemView.findViewById(R.id.tvMealCount);
            tvCarbs = itemView.findViewById(R.id.tvCarbs);
            tvProtein = itemView.findViewById(R.id.tvProtein);
            tvFat = itemView.findViewById(R.id.tvFat);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
        }
    }
}
