package com.example.injehealth.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "diet_logs")
public class DietLog {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String date;       // "yyyy-MM-dd"
    public String meal_type;  // "아침" | "점심" | "저녁" | "간식"
    public String memo;
    public String photo_path;
    public String created_at;
}
