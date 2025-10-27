package edu.illinois.cs.cs124.ay2025.mp.application;

import android.app.Application;
import android.os.Build;
import edu.illinois.cs.cs124.ay2025.mp.network.Client;
import edu.illinois.cs.cs124.ay2025.mp.network.Server;

public final class EventableApplication extends Application {
  public static final int DEFAULT_SERVER_PORT = 8024;
  public static final String SERVER_URL = "http://localhost:" + DEFAULT_SERVER_PORT;
  private static final long SERVER_STARTUP_TIMEOUT_MS = 8000;
  private Client client;

  @Override
  public void onCreate() {
    super.onCreate();

    if (!Build.FINGERPRINT.equals("robolectric")) {
      Thread serverThread = new Thread(Server::start);
      serverThread.start();
      try {
        serverThread.join(SERVER_STARTUP_TIMEOUT_MS);
        if (serverThread.isAlive()) {
          throw new IllegalStateException(
              "Server failed to start within " + (SERVER_STARTUP_TIMEOUT_MS / 1000) + " seconds");
        }
      } catch (InterruptedException e) {
        throw new IllegalStateException("Server startup interrupted", e);
      }
    }

    client = Client.start();
  }

  public Client getClient() {
    return client;
  }
}
