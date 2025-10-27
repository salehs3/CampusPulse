package edu.illinois.cs.cs124.ay2025.mp.test.helpers;

import static edu.illinois.cs.cs124.ay2025.mp.helpers.Helpers.getTimeProvider;

import androidx.annotation.NonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import edu.illinois.cs.cs124.ay2025.mp.models.EventData;
import edu.illinois.cs.cs124.ay2025.mp.models.Summary;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Data {
  @NonNull
  public static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));

  @NonNull private static final String EVENTS_FINGERPRINT = "feb8683086258d7881e2d3c0d2f86952";
  @NonNull public static final List<EventData> EVENT_DATA = loadEventData();
  @NonNull public static final List<Summary> SUMMARIES = loadSummaries();
  public static final int SUMMARY_COUNT = SUMMARIES.size();

  public static int getSummaryCountFromToday() {
    Instant now = getTimeProvider().now();
    ZonedDateTime nowZoned = now.atZone(ZoneId.of("America/Chicago"));
    ZonedDateTime startOfToday = nowZoned.toLocalDate().atStartOfDay(ZoneId.of("America/Chicago"));

    return (int)
        SUMMARIES.stream()
            .filter(
                summary -> {
                  try {
                    ZonedDateTime eventStart = ZonedDateTime.parse(summary.getStart());
                    return !eventStart.isBefore(startOfToday);
                  } catch (Exception e) {
                    return true;
                  }
                })
            .count();
  }

  public static int getSummaryCountToday() {
    Instant now = getTimeProvider().now();
    ZonedDateTime nowZoned = now.atZone(ZoneId.of("America/Chicago"));
    ZonedDateTime startOfToday = nowZoned.toLocalDate().atStartOfDay(ZoneId.of("America/Chicago"));
    ZonedDateTime startOfTomorrow = startOfToday.plusDays(1);

    return (int)
        SUMMARIES.stream()
            .filter(
                summary -> {
                  try {
                    ZonedDateTime eventStart =
                        ZonedDateTime.parse(summary.getStart())
                            .withZoneSameInstant(ZoneId.of("America/Chicago"));
                    return !eventStart.isBefore(startOfToday)
                        && eventStart.isBefore(startOfTomorrow);
                  } catch (Exception e) {
                    return false;
                  }
                })
            .count();
  }

  @NonNull
  public static List<EventData> loadEventData() {
    String json = loadAndFingerprintJSON();
    try {
      com.fasterxml.jackson.databind.JsonNode root = OBJECT_MAPPER.readTree(json);
      com.fasterxml.jackson.databind.JsonNode eventsArray = root.get("events");
      List<EventData> events = OBJECT_MAPPER.convertValue(eventsArray, new TypeReference<>() {});
      return Collections.unmodifiableList(events);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  @NonNull
  public static List<Summary> loadSummaries() {
    return loadEventData().stream().map(Summary::new).toList();
  }

  @NonNull
  public static String loadAndFingerprintJSON() {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("MD5");
    } catch (Exception e) {
      throw new IllegalStateException("MD5 algorithm should be available", e);
    }

    String input = readEventDataFile();

    String toFingerprint =
        Arrays.stream(input.split("\n"))
            .map(String::stripTrailing)
            .collect(Collectors.joining("\n"));

    String currentFingerprint =
        String.format(
                "%1$32s",
                new BigInteger(1, digest.digest(toFingerprint.getBytes(StandardCharsets.UTF_8)))
                    .toString(16))
            .replace(' ', '0');

    if (!currentFingerprint.equals(EVENTS_FINGERPRINT)) {
      throw new IllegalStateException(
          "events.json has been modified. Please restore the original version of the file.");
    }
    return input;
  }

  @NonNull
  public static List<Summary> getShuffledSummaries(int seed, int size) {
    List<Summary> trimmedSummaries = new ArrayList<>(SUMMARIES);
    Collections.shuffle(trimmedSummaries, new Random(seed));
    return trimmedSummaries.subList(0, Math.min(size, SUMMARY_COUNT));
  }

  private static String readEventDataFile() {
    return new Scanner(Data.class.getResourceAsStream("/events.json"), StandardCharsets.UTF_8)
        .useDelimiter("\\A")
        .next();
  }
}
