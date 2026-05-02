package com.example.injehealth.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.injehealth.db.entity.DietLog;

import java.util.List;

@Dao
public interface DietLogDao {
    @Insert
    long insert(DietLog log);

    @Update
    void update(DietLog log);

    @Delete
    void delete(DietLog log);

    @Query("SELECT * FROM diet_logs ORDER BY date DESC, meal_type ASC")
    List<DietLog> getAll();

    @Query("SELECT * FROM diet_logs WHERE date = :date ORDER BY meal_type ASC")
    List<DietLog> getByDate(String date);

    @Query("SELECT * FROM diet_logs WHERE date = :date AND meal_type = :mealType LIMIT 1")
    DietLog getByDateAndMealType(String date, String mealType);

    @Query("SELECT DISTINCT date FROM diet_logs ORDER BY date DESC")
    List<String> getAllDates();
}
