package com.example.injehealth.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.injehealth.db.entity.BodyRecord;

import java.util.List;

@Dao
public interface BodyRecordDao {
    @Insert
    void insert(BodyRecord record);

    @Update
    void update(BodyRecord record);

    @Delete
    void delete(BodyRecord record);

    @Query("SELECT * FROM body_records ORDER BY date DESC")
    List<BodyRecord> getAll();

    @Query("SELECT * FROM body_records WHERE date = :date LIMIT 1")
    BodyRecord getByDate(String date);
}
