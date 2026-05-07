package com.example.injehealth.db;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.injehealth.db.dao.BodyRecordDao;
import com.example.injehealth.db.dao.DietItemDao;
import com.example.injehealth.db.dao.DietLogDao;
import com.example.injehealth.db.dao.ExerciseDao;
import com.example.injehealth.db.dao.RoutineDao;
import com.example.injehealth.db.dao.UserDao;
import com.example.injehealth.db.dao.WorkoutLogDao;
import com.example.injehealth.db.dao.WorkoutSessionDao;
import com.example.injehealth.db.entity.BodyRecord;
import com.example.injehealth.db.entity.DietItem;
import com.example.injehealth.db.entity.DietLog;
import com.example.injehealth.db.entity.Exercise;
import com.example.injehealth.db.entity.Routine;
import com.example.injehealth.db.entity.User;
import com.example.injehealth.db.entity.WorkoutLog;
import com.example.injehealth.db.entity.WorkoutSession;

@Database(
        entities = {
                User.class,
                Exercise.class,
                Routine.class,
                WorkoutSession.class,
                WorkoutLog.class,
                BodyRecord.class,
                DietLog.class,
                DietItem.class
        },
        version = 5
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract ExerciseDao exerciseDao();
    public abstract RoutineDao routineDao();
    public abstract WorkoutSessionDao workoutSessionDao();
    public abstract WorkoutLogDao workoutLogDao();
    public abstract BodyRecordDao bodyRecordDao();
    public abstract DietLogDao dietLogDao();
    public abstract DietItemDao dietItemDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `diet_logs` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`date` TEXT, " +
                "`meal_type` TEXT, " +
                "`memo` TEXT, " +
                "`photo_path` TEXT, " +
                "`created_at` TEXT)"
            );
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `diet_items` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`log_id` INTEGER NOT NULL, " +
                "`food_name` TEXT, " +
                "`amount` REAL NOT NULL, " +
                "`unit` TEXT, " +
                "`kcal` REAL NOT NULL, " +
                "`carbs` REAL NOT NULL, " +
                "`protein` REAL NOT NULL, " +
                "`fat` REAL NOT NULL, " +
                "FOREIGN KEY(`log_id`) REFERENCES `diet_logs`(`id`) ON DELETE CASCADE)"
            );
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_diet_items_log_id` ON `diet_items` (`log_id`)"
            );
        }
    };
    static final Migration MIGRATION_2_3 = new Migration(2,3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase supportSQLiteDatabase) {
            supportSQLiteDatabase.execSQL("ALTER TABLE `users` ADD COLUMN `name` TEXT");
        }
    };
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                "CREATE TABLE diet_logs_new (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "eaten_at TEXT NOT NULL, " +
                "photo_path TEXT, " +
                "memo TEXT)"
            );
            database.execSQL(
                "INSERT INTO diet_logs_new (id, eaten_at, photo_path, memo) " +
                "SELECT id, " +
                "date || ' ' || CASE meal_type " +
                "WHEN '아침' THEN '08:00' " +
                "WHEN '점심' THEN '12:30' " +
                "WHEN '저녁' THEN '19:00' " +
                "WHEN '간식' THEN '15:30' " +
                "ELSE '12:00' END, " +
                "photo_path, memo " +
                "FROM diet_logs"
            );
            database.execSQL("DROP TABLE diet_logs");
            database.execSQL("ALTER TABLE diet_logs_new RENAME TO diet_logs");
            database.execSQL("DROP INDEX IF EXISTS idx_diet_logs_eaten_at");
            database.execSQL("CREATE INDEX index_diet_logs_eaten_at ON diet_logs(eaten_at)");
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                "CREATE TABLE body_records_new (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "recorded_at TEXT NOT NULL, " +
                "weight REAL NOT NULL, " +
                "muscle_mass REAL NOT NULL, " +
                "body_fat_mass REAL NOT NULL, " +
                "body_fat_rate REAL NOT NULL, " +
                "memo TEXT)"
            );
            database.execSQL(
                "INSERT INTO body_records_new (id, recorded_at, weight, muscle_mass, body_fat_mass, body_fat_rate, memo) " +
                "SELECT id, date || ' 00:00', weight, muscle_mass, body_fat_mass, body_fat_rate, memo " +
                "FROM body_records"
            );
            database.execSQL("DROP TABLE body_records");
            database.execSQL("ALTER TABLE body_records_new RENAME TO body_records");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "inje_health.db"
                            )
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                            .addCallback(prepopulateCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final RoomDatabase.Callback prepopulateCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            seedDefaultExercises(db);
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            // 방어적 재시딩: 기본 운동 drawable 이미지가 누락되었으면 재삽입
            Cursor c = db.query("SELECT COUNT(*) FROM exercises WHERE is_custom=0 AND image_type='drawable'", new Object[0]);
            int count = 0;
            if (c.moveToFirst()) count = c.getInt(0);
            c.close();
            if (count < 28) {
                db.execSQL("DELETE FROM exercises WHERE is_custom=0");
                seedDefaultExercises(db);
            }
        }
    };

    private static void seedDefaultExercises(@NonNull SupportSQLiteDatabase db) {
        db.beginTransaction();
        try {
            // chest (가슴)
            insertExercise(db, "벤치프레스",              "가슴",   "chest_image");
            insertExercise(db, "인클라인 벤치프레스",      "가슴",   "chest_image");
            insertExercise(db, "덤벨 플라이",              "가슴",   "chest_image");
            insertExercise(db, "케이블 크로스오버",         "가슴",   "chest_image");
            insertExercise(db, "디클라인 벤치프레스",       "가슴",   "chest_image");
            insertExercise(db, "덤벨 벤치프레스",           "가슴",   "chest_image");
            insertExercise(db, "인클라인 덤벨 벤치프레스",  "가슴",   "chest_image");
            // back (등)
            insertExercise(db, "데드리프트",               "등",     "back_image");
            insertExercise(db, "랫풀다운",                 "등",     "back_image");
            insertExercise(db, "바벨 로우",                "등",     "back_image");
            insertExercise(db, "시티드 로우",              "등",     "back_image");
            // legs (하체)
            insertExercise(db, "스쿼트",                   "하체",   "leg_image");
            insertExercise(db, "레그프레스",               "하체",   "leg_image");
            insertExercise(db, "레그컬",                   "하체",   "leg_image");
            insertExercise(db, "레그익스텐션",             "하체",   "leg_image");
            // shoulders (어깨)
            insertExercise(db, "오버헤드 프레스",          "어깨",   "shoulder_image");
            insertExercise(db, "사이드 레터럴 레이즈",     "어깨",   "shoulder_image");
            insertExercise(db, "프론트 레이즈",            "어깨",   "shoulder_image");
            insertExercise(db, "리어 델트 플라이",         "어깨",   "shoulder_image");
            // arms (팔)
            insertExercise(db, "바벨컬",                   "팔",     "arm_image");
            insertExercise(db, "트라이셉스 익스텐션",      "팔",     "arm_image");
            insertExercise(db, "해머컬",                   "팔",     "arm_image");
            insertExercise(db, "케이블 푸시다운",          "팔",     "arm_image");
            // cardio (유산소)
            insertExercise(db, "러닝",                     "유산소", "cardio_image");
            insertExercise(db, "사이클",                   "유산소", "cardio_image");
            insertExercise(db, "로잉머신",                 "유산소", "cardio_image");
            insertExercise(db, "계단오르기",               "유산소", "cardio_image");
            insertExercise(db, "버피",                     "유산소", "cardio_image");
            insertExercise(db, "점핑잭",                   "유산소", "cardio_image");
            insertExercise(db, "줄넘기",                   "유산소", "cardio_image");
            insertExercise(db, "마운틴 클라이머",          "유산소", "cardio_image");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static void insertExercise(@NonNull SupportSQLiteDatabase db,
                                       String name, String bodyPart, String imageRef) {
        db.execSQL(
            "INSERT INTO exercises (name, body_part, image_type, image_ref, is_custom, description) " +
            "VALUES (?, ?, 'drawable', ?, 0, NULL)",
            new Object[]{name, bodyPart, imageRef}
        );
    }
}
