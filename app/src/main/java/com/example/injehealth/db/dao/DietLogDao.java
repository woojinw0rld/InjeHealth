package com.example.injehealth.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.injehealth.db.entity.DietLog;
import com.example.injehealth.db.model.DietDaySummary;

import java.util.List;

@Dao
public interface DietLogDao {
    @Insert
    long insert(DietLog log);

    @Update
    void update(DietLog log);

    @Delete
    void delete(DietLog log);

    @Query("SELECT * FROM diet_logs WHERE substr(eaten_at, 1, 10) = :date ORDER BY eaten_at ASC")
    List<DietLog> getByDate(String date);

    @Query("SELECT * FROM diet_logs WHERE id = :id")
    DietLog getById(long id);

    @Query("SELECT substr(dl.eaten_at, 1, 10) AS date, " +
           "COALESCE(SUM(di.kcal), 0) AS totalKcal, " +
           "COALESCE(SUM(di.carbs), 0) AS totalCarbs, " +
           "COALESCE(SUM(di.protein), 0) AS totalProtein, " +
           "COALESCE(SUM(di.fat), 0) AS totalFat, " +
           "COUNT(DISTINCT dl.id) AS mealCount, " +
           "(SELECT photo_path FROM diet_logs dl2 " +
           " WHERE substr(dl2.eaten_at, 1, 10) = substr(dl.eaten_at, 1, 10) " +
           " AND dl2.photo_path IS NOT NULL " +
           " ORDER BY dl2.eaten_at LIMIT 1) AS thumbnailPath " +
           "FROM diet_logs dl " +
           "LEFT JOIN diet_items di ON di.log_id = dl.id " +
           "GROUP BY date " +
           "ORDER BY date DESC")
    List<DietDaySummary> getDaySummaries();
}
