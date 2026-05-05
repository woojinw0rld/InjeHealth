package com.example.injehealth.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "diet_logs", indices = {@Index("eaten_at")})
public class DietLog {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    @ColumnInfo(name = "eaten_at")
    public String eatenAt = "";  // "yyyy-MM-dd HH:mm", NOT NULL

    @ColumnInfo(name = "photo_path")
    public String photoPath; // nullable

    public String memo;      // nullable
}
