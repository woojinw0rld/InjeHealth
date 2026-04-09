package com.example.injehealth.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.injehealth.db.model.SessionSummary;
import com.example.injehealth.db.entity.WorkoutSession;

import java.util.List;

@Dao
public interface WorkoutSessionDao {
    @Insert
    long insert(WorkoutSession session);   // 세션 id 반환

    @Update
    void update(WorkoutSession session);

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    List<WorkoutSession> getAll();

    @Query("SELECT * FROM workout_sessions WHERE date = :date")
    WorkoutSession getByDate(String date);

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    WorkoutSession getById(int id);

    @Query("SELECT ws.id, ws.date, ws.body_part, ws.photo_path, ws.created_at, ws.done_at, " +
           "COUNT(DISTINCT wl.exercise_name) AS exercise_count, " +
           "COUNT(wl.id) AS total_sets " +
           "FROM workout_sessions ws " +
           "LEFT JOIN workout_logs wl ON wl.session_id = ws.id " +
           "GROUP BY ws.id " +
           "ORDER BY ws.date DESC")
    List<SessionSummary> getSessionSummaries();
}
