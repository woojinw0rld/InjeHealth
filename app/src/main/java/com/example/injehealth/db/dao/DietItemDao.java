package com.example.injehealth.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.injehealth.db.entity.DietItem;

import java.util.List;

@Dao
public interface DietItemDao {
    @Insert
    long insert(DietItem item);

    @Update
    void update(DietItem item);

    @Delete
    void delete(DietItem item);

    @Query("SELECT * FROM diet_items WHERE log_id = :logId")
    List<DietItem> getByLogId(int logId);

    @Query("DELETE FROM diet_items WHERE log_id = :logId")
    void deleteByLogId(int logId);
}
