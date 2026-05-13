package com.example.injehealth.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.injehealth.R;
import com.example.injehealth.db.entity.Exercise;
import com.example.injehealth.util.BodyPartLabels;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ViewHolder> {

    public interface OnExerciseClick {
        void onClick(Exercise exercise);
    }

    private List<Exercise> items;
    private final OnExerciseClick callback;

    public ExerciseAdapter(List<Exercise> items, OnExerciseClick callback) {
        this.items = items != null ? items : new ArrayList<>();
        this.callback = callback;
    }

    /** 검색/필터 갱신용 */
    public void updateData(List<Exercise> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exercise, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Exercise ex = items.get(position);
        Context ctx = h.itemView.getContext();

        // 이름
        h.tvName.setText(ex.name);

        // 부위 한글 배지
        h.tvBodyPart.setText(BodyPartLabels.kr(ex.body_part));

        // 이미지 처리
        if ("emoji".equals(ex.image_type)) {
            h.ivExercise.setVisibility(View.GONE);
            h.tvEmoji.setVisibility(View.VISIBLE);
            h.tvEmoji.setText(ex.image_ref);
        } else if ("file".equals(ex.image_type) && ex.image_ref != null) {
            h.ivExercise.setVisibility(View.VISIBLE);
            h.tvEmoji.setVisibility(View.GONE);
            File imgFile = new File(ctx.getFilesDir(), ex.image_ref);
            Glide.with(ctx).load(imgFile).centerCrop().into(h.ivExercise);
        } else if ("drawable".equals(ex.image_type) && ex.image_ref != null) {
            h.ivExercise.setVisibility(View.VISIBLE);
            h.tvEmoji.setVisibility(View.GONE);
            int resId = ctx.getResources().getIdentifier(ex.image_ref, "drawable", ctx.getPackageName());
            Log.d("IMGCHECK", "image_ref: " + ex.image_ref + " resId: " + resId);
            if (resId != 0) {
                Glide.with(ctx).load(resId).centerCrop().into(h.ivExercise);
            } else {
                h.ivExercise.setImageResource(R.drawable.chest_image);
            }
        } else {
            h.ivExercise.setVisibility(View.VISIBLE);
            h.tvEmoji.setVisibility(View.GONE);
            h.ivExercise.setImageResource(R.drawable.chest_image);
        }

        // 커스텀 배지
        h.tvCustomBadge.setVisibility(ex.is_custom == 1 ? View.VISIBLE : View.GONE);

        // 클릭
        h.itemView.setOnClickListener(v -> {
            if (callback != null) callback.onClick(ex);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvBodyPart, tvEmoji, tvCustomBadge;
        ImageView ivExercise;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName        = itemView.findViewById(R.id.tvName);
            tvBodyPart    = itemView.findViewById(R.id.tvBodyPart);
            tvEmoji       = itemView.findViewById(R.id.tvEmoji);
            tvCustomBadge = itemView.findViewById(R.id.tvCustomBadge);
            ivExercise    = itemView.findViewById(R.id.ivExercise);
        }
    }
}
