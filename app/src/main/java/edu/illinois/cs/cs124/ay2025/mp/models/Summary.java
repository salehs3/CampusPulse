package edu.illinois.cs.cs124.ay2025.mp.models;

import androidx.annotation.NonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class Summary implements Comparable<Summary> {
  @NonNull private final String id;

  @NonNull private final String title;

  @NonNull
  public final String getTitle() {
    return title;
  }

  @NonNull private final String start;

  @NonNull
  public final String getStart() {
    return start;
  }

  @NonNull private final String location;

  @NonNull
  public final String getLocation() {
    return location;
  }

  private final boolean virtual;

  public final boolean getVirtual() {
    return virtual;
  }

  @NonNull
  public final String getId() {
    return id;
  }

  public Summary(@NonNull EventData eventData) {
    id = eventData.id();
    title = eventData.title();
    start = eventData.start();
    location = eventData.location();
    virtual = eventData.virtual();
  }

  @JsonCreator
  public Summary(
      @NonNull String setId,
      @NonNull String setTitle,
      @NonNull String setStart,
      @NonNull String setLocation,
      boolean setVirtual) {
    id = setId;
    title = setTitle;
    start = setStart;
    location = setLocation;
    virtual = setVirtual;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Summary other)) {
      return false;
    }
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public int compareTo(@NonNull Summary other) {
    return this.start.compareTo(other.start);
  }

  public static List<Summary> filterVirtual(List<Summary> summaries, boolean virtual) {
    return List.of();
  }

  public static List<Summary> filterTime(List<Summary> summaries, Instant start, Instant end) {
    return List.of();
  }

  public static List<Summary> search(List<Summary> summaries, String query) {
    return List.of();
  }
}
