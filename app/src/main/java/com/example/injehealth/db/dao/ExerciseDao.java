package com.example.injehealth.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.injehealth.db.entity.Exercise;

import java.util.List;

@Dao
public interface ExerciseDao {
    @Insert
    void insert(Exercise exercise);

    @Insert
    void insertAll(List<Exercise> exercises);

    @Delete
    void delete(Exercise exercise);

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    List<Exercise> getAll();

    @Query("SELECT * FROM exercises WHERE body_part = :bodyPart ORDER BY name ASC")
    List<Exercise> getByBodyPart(String bodyPart);

    @Query("SELECT * FROM exercises WHERE is_custom = 1")
    List<Exercise> getCustom();
}