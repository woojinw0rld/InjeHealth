package com.example.injehealth.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "exercises")
public class Exercise {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String body_part;
    public String image_type;   // "drawable" | "file"
    public String image_ref;
    public int is_custom;       // 0=기본, 1=커스텀
    public String description;
}