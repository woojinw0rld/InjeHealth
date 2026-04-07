package com.example.injehealth.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workout_sessions")
public class WorkoutSession {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String date;        // "yyyy-MM-dd"
    public String body_part;
    public String photo_path;
    public String created_at;  // ISO 8601
    public String done_at;
}
