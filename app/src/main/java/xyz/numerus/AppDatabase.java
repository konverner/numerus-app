package xyz.numerus;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {VideoClip.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract VideoClipDao videoClipDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "numerus_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
