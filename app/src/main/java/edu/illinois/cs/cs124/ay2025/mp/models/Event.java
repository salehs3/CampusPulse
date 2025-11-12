package edu.illinois.cs.cs124.ay2025.mp.models;

import androidx.annotation.NonNull;
import java.util.Arrays;
import java.util.List;

/**
 * Event - Represents a complete event with all details.
 *
 * <p>This class is similar to EventData but is used for the full event details returned from the
 * /event/{id} endpoint. It contains all the information about a specific event.
 */
public record Event(
    @NonNull String id,
    @NonNull String seriesId,
    @NonNull String title,
    @NonNull String start,
    @NonNull String location,
    @NonNull String description,
    @NonNull String[] categories,
    @NonNull String source,
    @NonNull String url,
    boolean virtual) {

  /**
   * Gets the event ID.
   *
   * @return the event ID
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the series ID.
   *
   * @return the series ID
   */
  public String getSeriesId() {
    return seriesId;
  }

  /**
   * Gets the event title.
   *
   * @return the event title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Gets the event start time.
   *
   * @return the event start time
   */
  public String getStart() {
    return start;
  }

  /**
   * Gets the event location.
   *
   * @return the event location
   */
  public String getLocation() {
    return location;
  }

  /**
   * Gets the event description.
   *
   * @return the event description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the event categories as a list.
   *
   * @return the event categories
   */
  public List<String> getCategories() {
    return Arrays.asList(categories);
  }

  /**
   * Gets the event source.
   *
   * @return the event source
   */
  public String getSource() {
    return source;
  }

  /**
   * Gets the event URL.
   *
   * @return the event URL
   */
  public String getUrl() {
    return url;
  }

  /**
   * Checks if the event is virtual.
   *
   * @return true if the event is virtual, false otherwise
   */
  public boolean isVirtual() {
    return virtual;
  }
}
