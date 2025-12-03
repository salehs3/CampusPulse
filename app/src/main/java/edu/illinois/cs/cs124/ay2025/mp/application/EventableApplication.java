package edu.illinois.cs.cs124.ay2025.mp.application;

import android.app.Application;
import android.os.Build;
import edu.illinois.cs.cs124.ay2025.mp.network.Client;
import edu.illinois.cs.cs124.ay2025.mp.network.Server;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * EventableApplication - The FIRST thing created when the app starts.
 *
 * <p>This class extends Application, which means Android creates it BEFORE any Activity (like
 * MainActivity). It lives for the entire lifetime of the app and is used to set up things that need
 * to be shared across the whole app.
 *
 * <p>STARTUP ORDER: 1. User taps app icon 2. Android creates EventableApplication → onCreate() runs
 * (THIS FILE!) 3. Server starts in background 4. Client is created 5. MainActivity is created and
 * displayed
 */
public final class EventableApplication extends Application {
  // The port number where our mock server will listen (like a phone number for the server)
  public static final int DEFAULT_SERVER_PORT = 8024;

  // The URL that the Client will use to talk to our local server
  public static final String SERVER_URL = "http://localhost:" + DEFAULT_SERVER_PORT;

  // How long to wait for the server to start before giving up (8 seconds)
  private static final long SERVER_STARTUP_TIMEOUT_MS = 8000;

  // The HTTP client that will make requests to fetch event data (created in onCreate)
  private Client client;

  // Cache of favorite event IDs (synchronized for thread safety across activities)
  private final Set<String> favoriteEventIds = Collections.synchronizedSet(new HashSet<>());

  /**
   * onCreate is called ONCE when the app first starts (before MainActivity is created). This is
   * where we start the mock server and create the HTTP client.
   */
  @Override
  public void onCreate() {
    super.onCreate(); // Always call parent class first

    // Check if we're running in a real app (not in automated tests)
    // "robolectric" is the testing framework, so this skips server startup during tests
    if (!Build.FINGERPRINT.equals("robolectric")) {
      // Start the mock web server in a separate thread (so it doesn't block the app startup)
      Thread serverThread = new Thread(Server::start);
      serverThread.start();

      try {
        // Wait for the server thread to finish starting up (with a timeout)
        serverThread.join(SERVER_STARTUP_TIMEOUT_MS);

        // If the server thread is still running after the timeout, something went wrong
        if (serverThread.isAlive()) {
          throw new IllegalStateException(
              "Server failed to start within " + (SERVER_STARTUP_TIMEOUT_MS / 1000) + " seconds");
        }
      } catch (InterruptedException e) {
        // This happens if something interrupts our waiting
        throw new IllegalStateException("Server startup interrupted", e);
      }
    }

    // Create the HTTP client that MainActivity will use to fetch event data
    client = Client.start();
  }

  /**
   * Returns the HTTP client so other parts of the app (like MainActivity) can use it. This is how
   * MainActivity gets access to the client to fetch event summaries.
   */
  public Client getClient() {
    return client;
  }

  /**
   * Updates the local favorite cache when a favorite status changes. This should be called whenever
   * setFavorite() is called, so the cache stays in sync with the server.
   *
   * @param eventId The ID of the event
   * @param isFavorite Whether the event is marked as favorite
   */
  public void updateFavoriteCache(String eventId, boolean isFavorite) {
    if (isFavorite) {
      favoriteEventIds.add(eventId);
    } else {
      favoriteEventIds.remove(eventId);
    }
  }

  /**
   * Checks if an event is in the local favorite cache.
   *
   * @param eventId The ID of the event to check
   * @return true if the event is cached as a favorite, false otherwise
   */
  public boolean isFavoriteCached(String eventId) {
    return favoriteEventIds.contains(eventId);
  }

  /**
   * Returns the set of favorite event IDs for filtering purposes.
   *
   * @return A synchronized set of favorite event IDs
   */
  public Set<String> getFavoriteEventIds() {
    return favoriteEventIds;
  }
}
