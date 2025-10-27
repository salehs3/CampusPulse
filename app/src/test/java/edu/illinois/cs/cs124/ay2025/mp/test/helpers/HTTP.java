package edu.illinois.cs.cs124.ay2025.mp.test.helpers;

import static com.google.common.truth.Truth.assertWithMessage;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import edu.illinois.cs.cs124.ay2025.mp.application.EventableApplication;
import edu.illinois.cs.cs124.ay2025.mp.helpers.ResultMightThrow;
import edu.illinois.cs.cs124.ay2025.mp.network.Client;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HTTP {
  public static class TimedResponse<T> {
    private final T response;
    private final Duration responseTime;

    public TimedResponse(T setResponse, Duration setResponseTime) {
      response = setResponse;
      responseTime = setResponseTime;
    }

    public T getResponse() {
      return response;
    }

    public Duration getResponseTime() {
      return responseTime;
    }
  }

  private static final OkHttpClient httpClient =
      new OkHttpClient.Builder().callTimeout(4, TimeUnit.SECONDS).build();

  @SuppressWarnings("unchecked")
  public static <T> TimedResponse<T> testServerGet(String route, int responseCode, Object klass)
      throws IOException {
    Request request = new Request.Builder().url(EventableApplication.SERVER_URL + route).build();
    Instant start = Instant.now();

    try (Response response = httpClient.newCall(request).execute()) {
      Duration responseTime = Duration.between(start, Instant.now());

      if (responseCode == HttpURLConnection.HTTP_OK) {
        assertWithMessage("GET request for " + route + " should have succeeded")
            .that(response.code())
            .isEqualTo(HttpURLConnection.HTTP_OK);
      } else {
        assertWithMessage(
                "GET request for " + route + " should have failed with code " + responseCode)
            .that(response.code())
            .isEqualTo(responseCode);
        return new TimedResponse<>(null, responseTime);
      }

      ResponseBody body = response.body();
      assertWithMessage("GET response for " + route + " body should not be null")
          .that(body)
          .isNotNull();

      if (klass == null) {
        String bodyString = body.string();
        try {
          return new TimedResponse<>((T) Data.OBJECT_MAPPER.readTree(bodyString), responseTime);
        } catch (JsonParseException unused) {
          return new TimedResponse<>((T) bodyString, responseTime);
        }
      }

      assertWithMessage("Content-Type header not set correctly")
          .that(response.header("Content-Type"))
          .isEqualTo("application/json; charset=utf-8");
      if (klass instanceof Class<?> it) {
        return new TimedResponse<>(
            (T) Data.OBJECT_MAPPER.readValue(body.string(), it), responseTime);
      } else if (klass instanceof TypeReference<?> it) {
        return new TimedResponse<>(
            (T) Data.OBJECT_MAPPER.readValue(body.string(), it), responseTime);
      } else {
        throw new IllegalStateException("Bad deserialization class passed to testServerGet");
      }
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T testServerGet(String route, Object klass) throws IOException {
    return (T) testServerGet(route, HttpURLConnection.HTTP_OK, klass).getResponse();
  }

  public static <T> TimedResponse<T> testServerGetTimed(String route, Object klass)
      throws IOException {
    return testServerGet(route, HttpURLConnection.HTTP_OK, klass);
  }

  @SuppressWarnings("unchecked")
  public static <T> T testServerGet(String route, int responseCode) throws IOException {
    return (T) testServerGet(route, responseCode, null).getResponse();
  }

  public static JsonNode testServerGet(String route) throws IOException {
    return (JsonNode) testServerGet(route, HttpURLConnection.HTTP_OK, null).getResponse();
  }

  public static <T> TimedResponse<T> testServerGetTimed(String route) throws IOException {
    return testServerGet(route, HttpURLConnection.HTTP_OK, null);
  }

  @SuppressWarnings("unchecked")
  public static <T> T testServerPost(
      String route, int responseCode, Object requestBody, Object klass) throws IOException {
    Request request =
        new Request.Builder()
            .url(EventableApplication.SERVER_URL + route)
            .post(
                RequestBody.create(
                    Data.OBJECT_MAPPER.writeValueAsString(requestBody),
                    MediaType.parse("application/json")))
            .build();

    try (Response response = httpClient.newCall(request).execute()) {

      if (responseCode == HttpURLConnection.HTTP_OK) {
        assertWithMessage(
                "POST request for " + route + " should have succeeded but was " + response.code())
            .that(response.code())
            .isEqualTo(HttpURLConnection.HTTP_OK);
      } else {
        assertWithMessage(
                "POST request for " + route + " should have failed with code " + responseCode)
            .that(response.code())
            .isEqualTo(responseCode);
        return null;
      }

      ResponseBody responseBody = response.body();
      assertWithMessage("POST response for " + route + " body should not be null")
          .that(responseBody)
          .isNotNull();

      assertWithMessage("Content-Type header not set correctly")
          .that(response.header("Content-Type"))
          .isEqualTo("application/json; charset=utf-8");

      return switch (klass) {
        case null -> (T) Data.OBJECT_MAPPER.readTree(responseBody.string());
        case Class<?> it -> (T) Data.OBJECT_MAPPER.readValue(responseBody.string(), it);
        case TypeReference<?> it -> (T) Data.OBJECT_MAPPER.readValue(responseBody.string(), it);
        default ->
            throw new IllegalStateException("Bad deserialization class passed to testServerPost");
      };
    }
  }

  public static <T> T testServerPost(String route, Object requestBody, Object klass)
      throws IOException {
    return testServerPost(route, HttpURLConnection.HTTP_OK, requestBody, klass);
  }

  public static <T> T testServerPost(String route, Object requestBody, int responseCode)
      throws IOException {
    return testServerPost(route, responseCode, requestBody, null);
  }

  private static Client apiClient = null;

  public static Client getAPIClient() {
    if (apiClient == null) {
      apiClient = Client.start();
    }
    return apiClient;
  }

  public static <T> T testClient(Consumer<Consumer<ResultMightThrow<T>>> method) throws Exception {
    CompletableFuture<ResultMightThrow<T>> completableFuture = new CompletableFuture<>();

    method.accept(completableFuture::complete);

    ResultMightThrow<T> result = completableFuture.get(6, TimeUnit.SECONDS);

    if (result.getException() != null) {
      throw result.getException();
    }

    assertWithMessage("Client call expected to succeed returned null")
        .that(result.getValue())
        .isNotNull();

    return result.getValue();
  }
}
