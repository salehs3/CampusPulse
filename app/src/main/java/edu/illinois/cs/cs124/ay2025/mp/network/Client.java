package edu.illinois.cs.cs124.ay2025.mp.network;

import static edu.illinois.cs.cs124.ay2025.mp.helpers.Helpers.OBJECT_MAPPER;

import android.os.Build;
import androidx.annotation.NonNull;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.illinois.cs.cs124.ay2025.mp.application.EventableApplication;
import edu.illinois.cs.cs124.ay2025.mp.helpers.ResultMightThrow;
import edu.illinois.cs.cs124.ay2025.mp.models.Event;
import edu.illinois.cs.cs124.ay2025.mp.models.Favorite;
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
import okhttp3.RequestBody;
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

  // HTTP status code for temporary redirect
  private static final int HTTP_REDIRECT = 302;

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

  /**
   * Fetches a single event by its ID from the server asynchronously.
   *
   * <p>This method runs the network request on a background thread (using the executor) so it
   * doesn't freeze the UI. When the request completes, it calls the callback with the result.
   *
   * @param eventId The ID of the event to fetch
   * @param callback A function that gets called when the request finishes. It receives either the
   *     Event object (on success) or an error (on failure) wrapped in ResultMightThrow.
   */
  public void getEvent(
      @NonNull final String eventId, @NonNull final Consumer<ResultMightThrow<Event>> callback) {
    // Execute this code on a background thread (not the main UI thread)
    executor.execute(
        () -> {
          try {
            // Build the HTTP GET request to the /event/{id} endpoint
            Request request =
                new Request.Builder()
                    .url(EventableApplication.SERVER_URL + "/event/" + eventId)
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

              // Convert the JSON string into an Event object using Jackson
              Event event = OBJECT_MAPPER.readValue(responseBody, Event.class);

              // Success! Call the callback with the event
              callback.accept(new ResultMightThrow<>(event));
            }
          } catch (IOException e) {
            // If anything goes wrong (network error, parsing error, etc.), call callback with error
            callback.accept(new ResultMightThrow<>(e));
          }
        });
  }

  /**
   * Fetches all favorite event IDs from the server asynchronously.
   *
   * <p>This method runs the network request on a background thread so it doesn't freeze the UI.
   * When the request completes, it calls the callback with the list of favorite event IDs.
   *
   * @param callback A function that gets called when the request finishes. It receives either a
   *     list of favorite event IDs (on success) or an error (on failure) wrapped in
   *     ResultMightThrow.
   */
  public void getAllFavorites(@NonNull final Consumer<ResultMightThrow<List<String>>> callback) {
    // Execute this code on a background thread
    executor.execute(
        () -> {
          try {
            // Build the HTTP GET request to the /favorite endpoint
            Request request =
                new Request.Builder()
                    .url(EventableApplication.SERVER_URL + "/favorite")
                    .get()
                    .build();

            // Execute the request and get the response
            try (Response response = httpClient.newCall(request).execute()) {
              // Check if the request was successful
              if (!response.isSuccessful()) {
                callback.accept(
                    new ResultMightThrow<>(
                        new IOException("Unexpected response code: " + response.code())));
                return;
              }

              // Get the response body as a string (JSON array of event IDs)
              String responseBody = response.body().string();

              // Convert the JSON array into a List of Strings
              List<String> favoriteIds =
                  OBJECT_MAPPER.readValue(
                      responseBody,
                      OBJECT_MAPPER
                          .getTypeFactory()
                          .constructCollectionType(List.class, String.class));

              // Call the callback with the successful result
              callback.accept(new ResultMightThrow<>(favoriteIds));
            }
          } catch (IOException e) {
            // If anything goes wrong, call callback with error
            callback.accept(new ResultMightThrow<>(e));
          }
        });
  }

  /**
   * Fetches the favorite status for a specific event by its ID from the server asynchronously.
   *
   * <p>This method runs the network request on a background thread (using the executor) so it
   * doesn't freeze the UI. When the request completes, it calls the callback with the result.
   *
   * @param eventId The ID of the event to get favorite status for
   * @param callback A function that gets called when the request finishes. It receives either the
   *     favorite status (true/false) (on success) or an error (on failure) wrapped in
   *     ResultMightThrow.
   */
  public void getFavorite(
      @NonNull final String eventId, @NonNull final Consumer<ResultMightThrow<Boolean>> callback) {
    // Execute this code on a background thread (not the main UI thread)
    executor.execute(
        () -> {
          try {
            // Build the HTTP GET request to the /favorite/{id} endpoint
            Request request =
                new Request.Builder()
                    .url(EventableApplication.SERVER_URL + "/favorite/" + eventId)
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

              // Convert the JSON string into a Favorite object using Jackson
              Favorite favorite = OBJECT_MAPPER.readValue(responseBody, Favorite.class);

              // Success! Call the callback with the favorite status
              callback.accept(new ResultMightThrow<>(favorite.getFavorite()));
            }
          } catch (IOException e) {
            // If anything goes wrong (network error, parsing error, etc.), call callback with error
            callback.accept(new ResultMightThrow<>(e));
          }
        });
  }

  /**
   * Sets the favorite status for a specific event by its ID on the server asynchronously.
   *
   * <p>This method runs the network request on a background thread (using the executor) so it
   * doesn't freeze the UI. When the request completes, it calls the callback with the result.
   *
   * @param eventId The ID of the event to set favorite status for
   * @param isFavorite Whether the event should be marked as favorite (true) or not (false)
   * @param callback A function that gets called when the request finishes. It receives either the
   *     favorite status that was set (true/false) (on success) or an error (on failure) wrapped in
   *     ResultMightThrow.
   */
  public void setFavorite(
      @NonNull final String eventId,
      final boolean isFavorite,
      @NonNull final Consumer<ResultMightThrow<Boolean>> callback) {
    // Execute this code on a background thread (not the main UI thread)
    executor.execute(
        () -> {
          try {
            // Create a Favorite object with the event ID and favorite status
            Favorite favorite = new Favorite(eventId, isFavorite);

            // Convert the Favorite object to JSON string
            String jsonBody = OBJECT_MAPPER.writeValueAsString(favorite);

            // Create the request body
            RequestBody requestBody = RequestBody.create(jsonBody, JSON);

            // Build the HTTP POST request to the /favorite endpoint
            Request request =
                new Request.Builder()
                    .url(EventableApplication.SERVER_URL + "/favorite/")
                    .post(requestBody)
                    .build();

            // Execute the request and get the response (try-with-resources auto-closes response)
            try (Response response = httpClient.newCall(request).execute()) {
              // Check if the request was successful (HTTP 302 redirect or 200)
              if (!response.isSuccessful() && response.code() != HTTP_REDIRECT) {
                // If not successful, call the callback with an error
                callback.accept(
                    new ResultMightThrow<>(
                        new IOException("Unexpected response code: " + response.code())));
                return;
              }

              // Success! Call the callback with the favorite status that was set
              callback.accept(new ResultMightThrow<>(isFavorite));
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
