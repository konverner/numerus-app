package xyz.numerus;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "video_clips")
public class VideoClip {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @SerializedName("id")
    @ColumnInfo(name = "remote_id")
    public int remoteId;

    @SerializedName("url")
    @ColumnInfo(name = "video_id")
    public String videoId;

    @SerializedName("time")
    @ColumnInfo(name = "start_time")
    public float startTime;

    @SerializedName("subs")
    @ColumnInfo(name = "captions")
    public String captions;

    @ColumnInfo(name = "language")
    public String language;
}
