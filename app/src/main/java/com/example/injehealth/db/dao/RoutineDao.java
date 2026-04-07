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

    @Query("SELECT * FROM routines WHERE body_part = :bodyPart")
    List<Routine> getByBodyPart(String bodyPart);

    @Query("DELETE FROM routines WHERE body_part = :bodyPart")
    void deleteByBodyPart(String bodyPart);
}