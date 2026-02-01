package xyz.numerus;

import android.graphics.drawable.ColorDrawable;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.PopupWindow;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import android.widget.EditText;
import android.widget.TextView;
import android.view.inputmethod.EditorInfo;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Response;
import android.content.SharedPreferences;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "NumerusDebug";
    private static final List<String> SUPPORTED_LANGUAGES = java.util.Arrays.asList("en", "fr", "ru", "de", "es");
    private Toolbar toolbar;
    private TextView captionsView;
    private TextView responseView;
    private EditText inputText;
    private boolean isCheckState = true;
    private String currentLanguage = "en";
    private static final String BASE_URL = "https://raw.githubusercontent.com/konverner/numerus-data/main/";

    private AppDatabase db;
    private RemoteApiService apiService;
    private YouTubePlayer activeYouTubePlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getDatabase(this);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(RemoteApiService.class);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextAppearance(this, R.style.RobotoBoldTextAppearance);

        captionsView = findViewById(R.id.captions);
        responseView = findViewById(R.id.response);
        inputText = findViewById(R.id.edit_text_input);
        inputText.setTextColor(ContextCompat.getColor(this, R.color.white));
        inputText.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
        MaterialButton buttonCheck = findViewById(R.id.button_check);

        inputText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                buttonCheck.performClick();
                return true;
            }
            return false;
        });

        // Set click listener on the Toolbar title
        toolbar.setOnClickListener(v -> showPopupWindow());

        toolbar.setOverflowIcon(ContextCompat.getDrawable(getApplicationContext(),R.drawable.language));

        YouTubePlayerView youTubePlayerView = findViewById(R.id.youtube_player_view);

        // Bootstrap data if DB is empty

        bootstrapData();
        // Sync data from remote
        syncData();

        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youtubePlayer) {
                activeYouTubePlayer = youtubePlayer;
                loadVideoFromDb(youtubePlayer);
            }
        });
    }

    private void loadVideoFromDb(@NonNull YouTubePlayer youtubePlayer) {
        loadVideoFromDb(youtubePlayer, 0);
    }

    private void loadVideoFromDb(@NonNull YouTubePlayer youtubePlayer, int retryCount) {
        Log.d(TAG, "loadVideoFromDb called for language: " + currentLanguage + " (retry: " + retryCount + ")");
        new Thread(() -> {
            VideoClip clip = db.videoClipDao().getRandomClip(currentLanguage);
            runOnUiThread(() -> {
                if (clip == null) {
                    if (retryCount < 5) {
                        Log.d(TAG, "No data yet, retrying in 1000ms...");
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> loadVideoFromDb(youtubePlayer, retryCount + 1), 1000);
                    } else {
                        Log.w(TAG, "No video clip found in DB for language: " + currentLanguage);
                        Toast.makeText(MainActivity.this, "No data for " + currentLanguage, Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                Log.d(TAG, "Loaded clip: " + clip.videoId + " at " + clip.startTime);

                String videoId = clip.videoId;
                float startTimeFloat = clip.startTime;
                String captions = clip.captions;

                // Load a video from the given timestamp
                youtubePlayer.loadVideo(videoId, startTimeFloat);
                captionsView.setText(captions);

                inputText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(inputText, InputMethodManager.SHOW_IMPLICIT);
                }

                findViewById(R.id.button_repeat).setOnClickListener(v -> youtubePlayer.seekTo(startTimeFloat));

                MaterialButton buttonCheck = findViewById(R.id.button_check);
                MaterialButton buttonReport = findViewById(R.id.button_report);

                buttonReport.setOnClickListener(v -> {
                    Toast.makeText(MainActivity.this, R.string.reported_message, Toast.LENGTH_SHORT).show();
                    buttonReport.setEnabled(false);
                    buttonReport.setAlpha(0.5f);
                });

                buttonCheck.setOnClickListener(v -> {
                    if (isCheckState) {
                        captionsView.setVisibility(View.VISIBLE);
                        String inputTextValue = String.valueOf(inputText.getText());

                        String numberPattern = "\\b" + inputTextValue + "\\b";

                        Pattern pattern = Pattern.compile(numberPattern);
                        Matcher matcher = pattern.matcher(captions);

                        if (matcher.find() && !inputTextValue.isEmpty()) {
                            responseView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.green));
                            responseView.setText(R.string.correct);
                            inputText.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.green));
                            inputText.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(MainActivity.this, R.color.green)));
                        } else {
                            responseView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.red));
                            responseView.setText(R.string.incorrect);
                            inputText.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.red));
                            inputText.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(MainActivity.this, R.color.red)));
                        }
                        responseView.setVisibility(View.VISIBLE);
                        buttonReport.setVisibility(View.VISIBLE);
                        buttonReport.setEnabled(true);
                        buttonReport.setAlpha(1.0f);
                        isCheckState = false;
                        buttonCheck.setIconResource(R.drawable.ic_next);
                        buttonCheck.setContentDescription(getString(R.string.next));
                    } else {
                        captionsView.setVisibility(View.GONE);
                        responseView.setVisibility(View.GONE);
                        buttonReport.setVisibility(View.GONE);
                        isCheckState = true;
                        buttonCheck.setIconResource(R.drawable.ic_check);
                        buttonCheck.setContentDescription(getString(R.string.check));
                        inputText.setText("");
                        inputText.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.white));
                        inputText.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(MainActivity.this, R.color.white)));
                        loadVideoFromDb(youtubePlayer);
                    }
                });
            });
        }).start();
    }

    private void bootstrapData() {
        Log.d(TAG, "bootstrapData sequence starting...");
        new Thread(() -> {
            try {
                int count = db.videoClipDao().getCount();
                Log.d(TAG, "Current DB count: " + count);
                if (count == 0) {
                    Gson gson = new Gson();
                    Type listType = new TypeToken<List<VideoClip>>(){}.getType();
                    for (String lang : SUPPORTED_LANGUAGES) {
                        try {
                            Log.d(TAG, "Bootstrapping language: " + lang);
                            String fileName = lang + ".json";
                            List<VideoClip> clips = gson.fromJson(new InputStreamReader(getAssets().open(fileName)), listType);
                            if (clips != null) {
                                for (VideoClip clip : clips) {
                                    clip.language = lang;
                                }
                                db.videoClipDao().insertAll(clips);
                                Log.d(TAG, "Inserted " + clips.size() + " clips for " + lang);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error bootstrapping " + lang, e);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Fatal error in bootstrap process", e);
            }
        }).start();
    }

    private void syncData() {
        Log.d(TAG, "syncData sequence starting from: " + BASE_URL);
        new Thread(() -> {
            try {
                Response<ManifestResponse> manifestResponse = apiService.getManifest().execute();
                if (manifestResponse.isSuccessful() && manifestResponse.body() != null) {
                    ManifestResponse manifest = manifestResponse.body();
                    Log.d(TAG, "Manifest fetched successfully");
                    if (manifest.languages == null) {
                        Log.w(TAG, "Manifest languages map is null");
                        return;
                    }
                    SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);

                    for (String lang : manifest.languages.keySet()) {
                        if (!SUPPORTED_LANGUAGES.contains(lang)) {
                            Log.d(TAG, "Skipping unsupported language: " + lang);
                            continue;
                        }
                        try {
                            ManifestResponse.LanguageInfo info = manifest.languages.get(lang);
                            if (info == null) continue;
                            int remoteVersion = info.version;
                            int localVersion = prefs.getInt("version_" + lang, 0);
                            Log.d(TAG, "Language: " + lang + ", Remote version: " + remoteVersion + ", Local version: " + localVersion);

                            if (remoteVersion > localVersion) {
                                Log.d(TAG, "New version detected for " + lang + ". Downloading from: " + info.url);
                                Response<List<VideoClip>> clipsResponse = apiService.getVideoClips(info.url).execute();
                                if (clipsResponse.isSuccessful() && clipsResponse.body() != null) {
                                    List<VideoClip> clips = clipsResponse.body();
                                    Log.d(TAG, "Downloaded " + clips.size() + " clips for " + lang);
                                    for (VideoClip clip : clips) {
                                        clip.language = lang;
                                    }
                                    db.runInTransaction(() -> {
                                        db.videoClipDao().deleteByLanguage(lang);
                                        db.videoClipDao().insertAll(clips);
                                    });
                                    prefs.edit().putInt("version_" + lang, remoteVersion).apply();
                                    Log.d(TAG, "Updated database and local version for " + lang);
                                } else {
                                    Log.e(TAG, "Failed to download clips for " + lang + " (URL: " + info.url + "): " + clipsResponse.code());
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error syncing language " + lang, e);
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to fetch manifest: " + manifestResponse.code());
                }
            } catch (Exception e) {
                Log.e(TAG, "General exception in syncData", e);
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void showPopupWindow() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.info_popup, toolbar, false);

        PopupWindow popupWindow = new PopupWindow(popupView, Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.showAsDropDown(toolbar, 0, 0, Gravity.END);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Update currentLanguage based on selection
        if (id == R.id.menu_english) {
            currentLanguage = "en";
        } else if (id == R.id.menu_french) {
            currentLanguage = "fr";
        } else if (id == R.id.menu_russian) {
            currentLanguage = "ru";
        } else if (id == R.id.menu_german) {
            currentLanguage = "de";
        } else if (id == R.id.menu_spanish) {
            currentLanguage = "es";
        } else {
            return super.onOptionsItemSelected(item);
        }

        // Reset UI state for the new language
        captionsView.setVisibility(View.GONE);
        responseView.setVisibility(View.GONE);
        findViewById(R.id.button_report).setVisibility(View.GONE);
        inputText.setText("");
        inputText.setTextColor(ContextCompat.getColor(this, R.color.white));
        inputText.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
        isCheckState = true;

        MaterialButton buttonCheck = findViewById(R.id.button_check);
        buttonCheck.setIconResource(R.drawable.ic_check);
        buttonCheck.setContentDescription(getString(R.string.check));

        // Load new video
        if (activeYouTubePlayer != null) {
            loadVideoFromDb(activeYouTubePlayer);
        }

        return true;
    }
}
