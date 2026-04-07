package com.example.injehealth.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "body_records")
public class BodyRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String date;          // "yyyy-MM-dd"
    public double weight;        // 체중 (kg)
    public double muscle_mass;   // 근육량 (kg)
    public double body_fat_mass; // 체지방량 (kg)
    public double body_fat_rate; // 체지방률 (%)
    public String memo;
}