package com.example.injehealth.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.injehealth.db.entity.BodyRecord;
import com.example.injehealth.db.entity.Exercise;
import com.example.injehealth.db.entity.Routine;
import com.example.injehealth.db.entity.User;
import com.example.injehealth.db.entity.WorkoutLog;
import com.example.injehealth.db.entity.WorkoutSession;
import com.example.injehealth.db.dao.BodyRecordDao;
import com.example.injehealth.db.dao.ExerciseDao;
import com.example.injehealth.db.dao.RoutineDao;
import com.example.injehealth.db.dao.UserDao;
import com.example.injehealth.db.dao.WorkoutLogDao;
import com.example.injehealth.db.dao.WorkoutSessionDao;

@androidx.room.Database(
        entities = {
                com.example.injehealth.db.entity.User.class,

                com.example.injehealth.db.entity.Exercise.class,

                com.example.injehealth.db.entity.Routine.class,
                com.example.injehealth.db.entity.WorkoutSession.class,

                com.example.injehealth.db.entity.WorkoutLog.class,

                com.example.injehealth.db.entity.BodyRecord.class
        },
        version = 1
)

public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract ExerciseDao exerciseDao();
    public abstract RoutineDao routineDao();
    public abstract WorkoutSessionDao workoutSessionDao();
    public abstract WorkoutLogDao workoutLogDao();
    public abstract BodyRecordDao bodyRecordDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "inje_health.db"
                            )
                            .addCallback(prepopulateCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    // 앱 최초 설치 시 기본 운동 종목 삽입
    private static final RoomDatabase.Callback prepopulateCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            // 필요 시 Executors로 기본 데이터 삽입
        }
    };
}