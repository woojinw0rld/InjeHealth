package com.example.injehealth.db;

import android.content.Context;

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
        version = 4
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

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "inje_health.db"
                            )
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
        }
    };
}
