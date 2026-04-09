package com.example.injehealth.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.injehealth.db.entity.WorkoutLog;

import java.util.List;

@Dao
public interface WorkoutLogDao {
    @Insert
    void insert(WorkoutLog log);

    @Insert
    void insertAll(List<WorkoutLog> logs);

    @Update
    void update(WorkoutLog log);

    @Query("SELECT * FROM workout_logs WHERE session_id = :sessionId ORDER BY exercise_name, set_number")
    List<WorkoutLog> getBySession(int sessionId);

    @Query("DELETE FROM workout_logs WHERE session_id = :sessionId")
    void deleteBySession(int sessionId);

    @Query("SELECT COUNT(DISTINCT exercise_name) FROM workout_logs WHERE session_id = :sessionId")
    int getExerciseCount(int sessionId);
}
