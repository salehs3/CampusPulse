package edu.illinois.cs.cs124.ay2025.mp.network;

import static edu.illinois.cs.cs124.ay2025.mp.helpers.Helpers.CHECK_SERVER_RESPONSE;
import static edu.illinois.cs.cs124.ay2025.mp.helpers.Helpers.OBJECT_MAPPER;
import static edu.illinois.cs.cs124.ay2025.mp.helpers.Helpers.getTimeProvider;

import androidx.annotation.NonNull;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import edu.illinois.cs.cs124.ay2025.mp.application.EventableApplication;
import edu.illinois.cs.cs124.ay2025.mp.models.Event;
import edu.illinois.cs.cs124.ay2025.mp.models.EventData;
import edu.illinois.cs.cs124.ay2025.mp.models.Summary;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public final class Server extends Dispatcher {
  private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
  private final List<Summary> summaries = new ArrayList<>();
  private final List<EventData> events = new ArrayList<>();

  private MockResponse makeOKJSONResponse(@NonNull String body) {
    return new MockResponse()
        .setResponseCode(HttpURLConnection.HTTP_OK)
        .setBody(body)
        .setHeader("Content-Type", "application/json; charset=utf-8");
  }

  private static final MockResponse HTTP_NOT_FOUND =
      new MockResponse()
          .setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
          .setBody("404: Not Found");

  private static final MockResponse HTTP_BAD_REQUEST =
      new MockResponse()
          .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
          .setBody("400: Bad Request");

  private MockResponse getSummaries() throws JsonProcessingException {
    Instant now = getTimeProvider().now();
    ZonedDateTime nowZoned = now.atZone(ZoneId.of("America/Chicago"));
    ZonedDateTime startOfToday = nowZoned.toLocalDate().atStartOfDay(ZoneId.of("America/Chicago"));

    List<Summary> filteredSummaries =
        summaries.stream()
            .filter(
                summary -> {
                  try {
                    ZonedDateTime eventStart = ZonedDateTime.parse(summary.getStart());
                    return !eventStart.isBefore(startOfToday);
                  } catch (Exception e) {
                    return true;
                  }
                })
            .toList();

    return makeOKJSONResponse(OBJECT_MAPPER.writeValueAsString(filteredSummaries));
  }

  private MockResponse getEvent(@NonNull String eventId) throws JsonProcessingException {
    // Loop through the events list to find the event with the matching ID
    for (EventData eventData : events) {
      if (eventData.id().equals(eventId)) {
        // Convert EventData to Event and return it
        Event event =
            new Event(
                eventData.id(),
                eventData.seriesId(),
                eventData.title(),
                eventData.start(),
                eventData.location(),
                eventData.description(),
                eventData.categories(),
                eventData.source(),
                eventData.url(),
                eventData.virtual());
        return makeOKJSONResponse(OBJECT_MAPPER.writeValueAsString(event));
      }
    }
    // If no event found with that ID, return 404
    return HTTP_NOT_FOUND;
  }

  @NonNull
  @Override
  public MockResponse dispatch(@NonNull RecordedRequest request) {
    if (request.getPath() == null || request.getMethod() == null) {
      return HTTP_BAD_REQUEST;
    }

    String path = request.getPath().replaceFirst("/*$", "").replaceAll("/+", "/");
    String method = request.getMethod().toUpperCase();

    try {
      if (path.isEmpty() && method.equals("GET")) {
        return makeOKJSONResponse(CHECK_SERVER_RESPONSE);
      } else if (path.equals("/reset") && method.equals("GET")) {
        return makeOKJSONResponse("200: OK");
      } else if (path.equals("/summary") && method.equals("GET")) {
        return getSummaries();
      } else if (path.startsWith("/event/") && method.equals("GET")) {
        // Extract the event ID from the path
        String eventId = path.substring("/event/".length());
        if (eventId.isEmpty()) {
          return HTTP_NOT_FOUND;
        }
        return getEvent(eventId);
      } else {
        return HTTP_NOT_FOUND;
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Server internal error for path: " + path, e);
      return new MockResponse()
          .setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
          .setBody("500: Internal Error");
    }
  }

  private void loadData() {
    String json = readEventDataFile();
    try {
      JsonNode root = OBJECT_MAPPER.readTree(json);
      JsonNode eventsArray = root.get("events");
      for (JsonNode node : eventsArray) {
        EventData eventData = OBJECT_MAPPER.readValue(node.toString(), EventData.class);
        Summary summary = new Summary(eventData);
        summaries.add(summary);
        events.add(eventData);
      }
    } catch (JsonProcessingException e) {
      LOGGER.log(Level.SEVERE, "Loading data failed", e);
      throw new IllegalStateException(e);
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // YOU SHOULD NOT NEED TO MODIFY THE CODE BELOW
  /////////////////////////////////////////////////////////////////////////////////////////////////

  private static Server server;
  private static MockWebServer mockWebServer;

  public static synchronized void start() {
    if (server == null) {
      server = new Server();
    }
    if (mockWebServer != null && isRunning(false)) {
      return;
    }
    try {
      if (mockWebServer != null) {
        mockWebServer.close();
      }
      mockWebServer = new MockWebServer();
      mockWebServer.setDispatcher(server);
      mockWebServer.start(EventableApplication.DEFAULT_SERVER_PORT);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Startup failed", e);
      throw new IllegalStateException(e);
    }
    if (!isRunning(true)) {
      throw new IllegalStateException("Server should be running");
    }
  }

  public static synchronized void stop() {
    if (mockWebServer != null) {
      try {
        mockWebServer.close();
      } catch (IOException ignored) {
      }
    }
    mockWebServer = null;
  }

  private static final int RETRY_COUNT = 8;
  private static final int RETRY_DELAY = 512;

  public static boolean isRunning(boolean wait) {
    return isRunning(wait, RETRY_COUNT, RETRY_DELAY);
  }

  public static boolean isRunning(boolean wait, int retryCount, long retryDelay) {
    for (int i = 0; i < retryCount; i++) {
      OkHttpClient client = new OkHttpClient();
      Request request = new Request.Builder().url(EventableApplication.SERVER_URL).get().build();
      try (Response response = client.newCall(request).execute()) {
        if (response.isSuccessful()) {
          if (Objects.requireNonNull(response.body()).string().equals(CHECK_SERVER_RESPONSE)) {
            return true;
          } else {
            throw new IllegalStateException(
                "Another server is running on port " + EventableApplication.DEFAULT_SERVER_PORT);
          }
        }
      } catch (IOException ignoredIOException) {
        if (!wait) {
          break;
        }
        try {
          Thread.sleep(retryDelay);
        } catch (InterruptedException ignoredInterruptedException) {
        }
      }
    }
    return false;
  }

  @SuppressWarnings("unused")
  public static boolean reset() throws IOException {
    OkHttpClient client = new OkHttpClient();
    Request request =
        new Request.Builder().url(EventableApplication.SERVER_URL + "/reset/").get().build();
    try (Response response = client.newCall(request).execute()) {
      return response.isSuccessful();
    }
  }

  private Server() {
    Logger.getLogger(MockWebServer.class.getName()).setLevel(Level.SEVERE);
    loadData();
  }

  private static String readEventDataFile() {
    return new Scanner(Server.class.getResourceAsStream("/events.json"), StandardCharsets.UTF_8)
        .useDelimiter("\\A")
        .next();
  }
}
