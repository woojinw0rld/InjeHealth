package com.example.injehealth.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "diet_items",
        foreignKeys = @ForeignKey(
                entity = DietLog.class,
                parentColumns = "id",
                childColumns = "log_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("log_id")
)
public class DietItem {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int log_id;        // FK → diet_logs.id
    public String food_name;
    public double amount;     // 섭취량
    public String unit;       // "g" | "ml" | "개"
    public double kcal;
    public double carbs;      // 탄수화물 (g)
    public double protein;    // 단백질 (g)
    public double fat;        // 지방 (g)
}
