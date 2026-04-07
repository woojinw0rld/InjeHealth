package com.example.injehealth.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "routines",
        foreignKeys = @ForeignKey(
                entity = Exercise.class,
                parentColumns = "id",
                childColumns = "exercise_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("exercise_id")}
)
public class Routine {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String body_part;
    public int exercise_id;
    public String exercise_name;
    public int default_sets;
    public int default_reps;
    public double default_weight;
}
