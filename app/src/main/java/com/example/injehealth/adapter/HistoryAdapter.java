package com.example.injehealth.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.injehealth.R;
import com.example.injehealth.db.model.SessionSummary;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 운동 기록 목록 RecyclerView 어댑터
 *
 * - SessionSummary(세션 요약 정보)를 item_history 레이아웃에 바인딩
 * - 날짜, 부위, 세트 수, 운동 시간, 종목 개수, 눈바디 썸네일 표시
 * - 아이템 클릭 시 OnItemClickListener 콜백 호출
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    /** 아이템 클릭 리스너 인터페이스 */
    public interface OnItemClickListener {
        void onItemClick(SessionSummary session);
    }

    private final List<SessionSummary> items = new ArrayList<>();
    private final OnItemClickListener listener;

    // 날짜 파싱/포맷용 (재사용을 위해 필드로 보관)
    private final SimpleDateFormat dateFormatIn = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat dateFormatOut = new SimpleDateFormat("yyyy년 M월 d일", Locale.getDefault());
    private final SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

    public HistoryAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * 데이터 전체 교체 후 갱신
     * @param newItems 새 세션 요약 리스트 (null 허용)
     */
    public void setItems(List<SessionSummary> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        SessionSummary session = items.get(position);
        holder.bind(session, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * 운동 기록 아이템 ViewHolder
     *
     * 바인딩 항목:
     * - 날짜: "yyyy-MM-dd" → "yyyy년 M월 d일"
     * - 부위 배지: body_part
     * - 세트 수 / 운동 시간 / 종목 개수
     * - 눈바디 썸네일: Glide로 파일 경로에서 로드
     */
    class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDate;
        private final TextView tvBodyPart;
        private final TextView tvSets;
        private final TextView tvTime;
        private final TextView tvExerciseCount;
        private final ImageView ivThumbnail;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvBodyPart = itemView.findViewById(R.id.tv_body_part);
            tvSets = itemView.findViewById(R.id.tv_sets);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvExerciseCount = itemView.findViewById(R.id.tv_exercise_count);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
        }

        public void bind(SessionSummary session, OnItemClickListener listener) {
            Context context = itemView.getContext();

            // 날짜 포맷 변환
            try {
                if (session.date != null) {
                    Date date = dateFormatIn.parse(session.date);
                    tvDate.setText(dateFormatOut.format(date));
                } else {
                    tvDate.setText("");
                }
            } catch (ParseException e) {
                tvDate.setText(session.date); // 파싱 실패 시 원본 문자열 표시
            }

            // 부위 배지
            tvBodyPart.setText(session.body_part != null ? session.body_part : "");

            // 세트 수
            tvSets.setText(String.format(Locale.getDefault(), context.getString(R.string.history_sets_format), session.total_sets));

            // 운동 시간: created_at ~ done_at 차이(분)
            if (session.created_at != null && session.done_at != null) {
                try {
                    Date created = isoFormat.parse(session.created_at);
                    Date done = isoFormat.parse(session.done_at);
                    if (created != null && done != null) {
                        long diffMs = done.getTime() - created.getTime();
                        long diffMins = diffMs / (60 * 1000);
                        tvTime.setText(String.format(Locale.getDefault(), context.getString(R.string.history_time_format), diffMins));
                    } else {
                        tvTime.setText("—");
                    }
                } catch (ParseException e) {
                    tvTime.setText("—");
                }
            } else {
                tvTime.setText("—");
            }

            // 운동 종목 개수
            tvExerciseCount.setText(String.format(Locale.getDefault(), context.getString(R.string.history_exercise_count_format), session.exercise_count));

            // 눈바디 썸네일: 사진 경로 있으면 Glide로 로드, 없으면 플레이스홀더
            if (session.photo_path != null && !session.photo_path.isEmpty()) {
                Glide.with(context)
                        .load(new File(session.photo_path))
                        .centerCrop()
                        .placeholder(R.drawable.ic_camera_placeholder)
                        .error(R.drawable.ic_camera_placeholder)
                        .into(ivThumbnail);
            } else {
                Glide.with(context).clear(ivThumbnail);
                ivThumbnail.setImageResource(R.drawable.ic_camera_placeholder);
            }

            // 아이템 클릭 → 상세 화면 이동
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(session);
                }
            });
        }
    }
}
