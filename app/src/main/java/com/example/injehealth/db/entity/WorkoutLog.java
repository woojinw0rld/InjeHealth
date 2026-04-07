package com.example.injehealth.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "workout_logs",
        foreignKeys = @ForeignKey(
                entity = WorkoutSession.class,
                parentColumns = "id",
                childColumns = "session_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("session_id")}
)
public class WorkoutLog {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int session_id;
    public String exercise_name;
    public int set_number;
    public int planned_sets;
    public int planned_reps;
    public double planned_weight;
    public int reps;
    public double weight;
    public int is_done;        // 0=미완, 1=완료
}
