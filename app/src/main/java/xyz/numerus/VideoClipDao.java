package xyz.numerus;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface VideoClipDao {
    @Query("SELECT * FROM video_clips WHERE language = :lang ORDER BY RANDOM() LIMIT 1")
    VideoClip getRandomClip(String lang);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<VideoClip> clips);

    @Query("SELECT COUNT(*) FROM video_clips")
    int getCount();

    @Query("DELETE FROM video_clips WHERE language = :lang")
    void deleteByLanguage(String lang);
}
