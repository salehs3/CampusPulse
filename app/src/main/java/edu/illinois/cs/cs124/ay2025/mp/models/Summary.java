package edu.illinois.cs.cs124.ay2025.mp.models;

import androidx.annotation.NonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Objects;

public class Summary {
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

  public Summary(@NonNull EventData eventData) {
    id = eventData.id();
    title = eventData.title();
    start = eventData.start();
    location = eventData.location();
  }

  @JsonCreator
  public Summary(
      @NonNull String setId,
      @NonNull String setTitle,
      @NonNull String setStart,
      @NonNull String setLocation) {
    id = setId;
    title = setTitle;
    start = setStart;
    location = setLocation;
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
}
