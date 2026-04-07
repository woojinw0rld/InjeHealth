package com.example.injehealth.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public double weight;
    public double height;
    public String gender;
    public int age;
}
