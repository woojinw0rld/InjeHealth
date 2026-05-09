package com.example.injehealth.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.injehealth.db.entity.Routine;

import java.util.List;

@Dao
public interface RoutineDao {
    @Insert
    void insert(Routine routine);

    @Insert
    void insertAll(List<Routine> routines);

    @Update
    void update(Routine routine);

    @Delete
    void delete(Routine routine);

    @Query("SELECT * FROM routines WHERE routine_name = :routine_name")
    List<Routine> getByRoutineName(String routine_name);

    @Query("SELECT * FROM routines ORDER BY routine_name, id")
    List<Routine> getAll();

    @Query("DELETE FROM routines WHERE routine_name = :routine_name")
    void deleteByBodyPart(String routine_name);

    @Query("SELECT DISTINCT routine_name FROM routines WHERE routine_name IS NOT + NULL ORDER BY routine_name")
    List<String> getDistinctRoutineNames();

    @Query("SELECT exercise_name FROM routines WHERE routine_name = :routineName")
    List<String> getExerciseNamesByRoutineName(String routineName);
}