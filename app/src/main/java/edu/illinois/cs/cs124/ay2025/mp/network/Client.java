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

/**
 * Client - Handles making HTTP requests to fetch event data from the server.
 *
 * <p>This class uses OkHttp library to make network requests. It's created during app startup by
 * EventableApplication and used by MainActivity to fetch event summaries.
 */
public final class Client {
  // Used for logging/debugging
  private static final String TAG = Client.class.getSimpleName();

  // Specifies that we're sending/receiving JSON data
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  /**
   * Fetches the list of event summaries from the server asynchronously.
   *
   * <p>This method runs the network request on a background thread (using the executor) so it
   * doesn't freeze the UI. When the request completes, it calls the callback with the results.
   *
   * @param callback A function that gets called when the request finishes. It receives either the
   *     list of summaries (on success) or an error (on failure) wrapped in ResultMightThrow.
   */
  public void getSummaries(@NonNull final Consumer<ResultMightThrow<List<Summary>>> callback) {
    // Execute this code on a background thread (not the main UI thread)
    executor.execute(
        () -> {
          try {
            // Build the HTTP GET request to the /summary/ endpoint
            Request request =
                new Request.Builder()
                    .url(EventableApplication.SERVER_URL + "/summary/")
                    .get()
                    .build();

            // Execute the request and get the response (try-with-resources auto-closes response)
            try (Response response = httpClient.newCall(request).execute()) {
              // Check if the request was successful (HTTP 200)
              if (!response.isSuccessful()) {
                // If not successful, call the callback with an error
                callback.accept(
                    new ResultMightThrow<>(
                        new IOException("Unexpected response code: " + response.code())));
                return;
              }

              // Get the response body as a string (JSON data)
              String responseBody = response.body().string();

              // Convert the JSON string into a List of Summary objects using Jackson
              List<Summary> summaries =
                  OBJECT_MAPPER.readValue(responseBody, new TypeReference<>() {});

              // Success! Call the callback with the list of summaries
              callback.accept(new ResultMightThrow<>(summaries));
            }
          } catch (IOException e) {
            // If anything goes wrong (network error, parsing error, etc.), call callback with error
            callback.accept(new ResultMightThrow<>(e));
          }
        });
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // YOU SHOULD NOT NEED TO MODIFY THE CODE BELOW
  /////////////////////////////////////////////////////////////////////////////////////////////////

  // Singleton pattern: only one Client instance exists for the entire app
  private static Client instance;

  // Network requests will timeout after 4 seconds if the server doesn't respond
  private static final int CALL_TIMEOUT_SECONDS = 4;

  // The OkHttp client that actually makes the HTTP requests
  private final OkHttpClient httpClient;

  // Manages background threads for running network requests
  private final ExecutorService executor;

  /**
   * Called during app startup by EventableApplication.onCreate(). Creates the Client if it doesn't
   * exist yet (singleton pattern).
   *
   * @return The single Client instance used throughout the app
   */
  @NonNull
  public static Client start() {
    // Only create the Client once, then reuse it
    if (instance == null) {
      instance = new Client();
    }
    return instance;
  }

  /**
   * Private constructor - called only once by start() during app startup. Sets up the HTTP client
   * and background thread executor.
   */
  private Client() {
    // Check if we're running in tests (robolectric) or in the real app
    boolean testing = Build.FINGERPRINT.equals("robolectric");

    // Configure the OkHttp client
    httpClient =
        new OkHttpClient.Builder()
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS) // Timeout after 4 seconds
            .retryOnConnectionFailure(true) // Automatically retry if connection fails
            .build();

    // Set up the thread pool for background tasks
    if (testing) {
      // In tests, use a single thread to make tests more predictable
      executor = Executors.newSingleThreadExecutor();
    } else {
      // In the real app, use a pool that grows/shrinks as needed for better performance
      executor = Executors.newCachedThreadPool();
    }
  }
}
