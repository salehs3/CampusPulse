package edu.illinois.cs.cs124.ay2025.mp.network;

import static edu.illinois.cs.cs124.ay2025.mp.helpers.Helpers.OBJECT_MAPPER;

import android.os.Build;
import androidx.annotation.NonNull;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.illinois.cs.cs124.ay2025.mp.application.EventableApplication;
import edu.illinois.cs.cs124.ay2025.mp.helpers.ResultMightThrow;
import edu.illinois.cs.cs124.ay2025.mp.models.Summary;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class Client {
  private static final String TAG = Client.class.getSimpleName();
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  public void getSummaries(@NonNull final Consumer<ResultMightThrow<List<Summary>>> callback) {
    executor.execute(
        () -> {
          try {
            Request request =
                new Request.Builder()
                    .url(EventableApplication.SERVER_URL + "/summary/")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
              if (!response.isSuccessful()) {
                callback.accept(
                    new ResultMightThrow<>(
                        new IOException("Unexpected response code: " + response.code())));
                return;
              }

              String responseBody = response.body().string();
              List<Summary> summaries =
                  OBJECT_MAPPER.readValue(responseBody, new TypeReference<>() {});
              callback.accept(new ResultMightThrow<>(summaries));
            }
          } catch (IOException e) {
            callback.accept(new ResultMightThrow<>(e));
          }
        });
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // YOU SHOULD NOT NEED TO MODIFY THE CODE BELOW
  /////////////////////////////////////////////////////////////////////////////////////////////////

  private static Client instance;
  private static final int CALL_TIMEOUT_SECONDS = 4;
  private final OkHttpClient httpClient;
  private final ExecutorService executor;

  @NonNull
  public static Client start() {
    if (instance == null) {
      instance = new Client();
    }
    return instance;
  }

  private Client() {
    boolean testing = Build.FINGERPRINT.equals("robolectric");

    httpClient =
        new OkHttpClient.Builder()
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    if (testing) {
      executor = Executors.newSingleThreadExecutor();
    } else {
      executor = Executors.newCachedThreadPool();
    }
  }
}
