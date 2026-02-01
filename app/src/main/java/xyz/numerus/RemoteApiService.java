package xyz.numerus;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

import retrofit2.http.Url;

public interface RemoteApiService {
    @GET("manifest.json")
    Call<ManifestResponse> getManifest();

    @GET
    Call<List<VideoClip>> getVideoClips(@Url String url);
}
