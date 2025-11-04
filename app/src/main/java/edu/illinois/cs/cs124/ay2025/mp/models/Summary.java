package edu.illinois.cs.cs124.ay2025.mp.models;

import androidx.annotation.NonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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
    int timeComparison = this.start.compareTo(other.start);
    if (timeComparison == 0) {
      // When times are equal, sort by title (case-sensitive)
      return this.title.compareTo(other.title);
    }
    return timeComparison;
  }

  public static List<Summary> filterVirtual(List<Summary> summaries, boolean virtual) {
    List<Summary> filteredSummaries = new ArrayList<>();
    for (Summary summary : summaries) {
      if (summary.getVirtual() == virtual) {
        filteredSummaries.add(summary);
      }
    }
    return  filteredSummaries;
  }

  public static List<Summary> filterTime(List<Summary> summaries, Instant start, Instant end) {
    List<Summary> filteredSummaries = new ArrayList<>();
    for (Summary summary : summaries) {
      Instant eventStartTime = Instant.parse(summary.getStart());

      boolean shouldInclude = false;

      if (start == null && end == null) {
        shouldInclude = true;
      } else if (start == null) {
        shouldInclude = eventStartTime.compareTo(end) <= 0;
      } else if (end == null) {
        shouldInclude = eventStartTime.compareTo(start) >= 0;
      } else {
        shouldInclude = eventStartTime.compareTo(end) <= 0 && eventStartTime.compareTo(start) >= 0;
      }

      if (shouldInclude) {
        filteredSummaries.add(summary);
      }
    }
    return  filteredSummaries;
  }

  public static List<Summary> search(List<Summary> summaries, String query) {
    String cleanedQuery = query.trim().toLowerCase();

    // Handle empty query - return all summaries sorted
    if (cleanedQuery.isEmpty()) {
      List<Summary> allSummaries = new ArrayList<>(summaries);
      Collections.sort(allSummaries);
      return allSummaries;
    }

    // Parse the query to extract filters and search text
    String searchText = "";
    String locationFilter = null;
    Boolean virtualFilter = null;
    boolean hasInvalidFilter = false;

    // Split query into tokens
    String[] tokens = cleanedQuery.split("\\s+");
    List<String> searchTokens = new ArrayList<>();

    for (int i = 0; i < tokens.length; i++) {
      String token = tokens[i];

      if (token.startsWith("location:")) {
        // Extract location filter value
        String locationValue = token.substring("location:".length());

        // Collect all following tokens until we hit another filter
        List<String> locationParts = new ArrayList<>();
        if (!locationValue.isEmpty()) {
          locationParts.add(locationValue);
        }

        // Look ahead for more location words
        for (int j = i + 1; j < tokens.length; j++) {
          if (tokens[j].startsWith("location:")
              || tokens[j].startsWith("virtual:")
              || tokens[j].contains(":")) {
            break;
          }
          locationParts.add(tokens[j]);
          i = j;
        }

        locationFilter = String.join(" ", locationParts).trim();

      } else if (token.startsWith("virtual:")) {
        // Extract virtual filter value
        String virtualValue = token.substring("virtual:".length());

        if (virtualValue.equals("true")) {
          virtualFilter = true;
        } else if (virtualValue.equals("false")) {
          virtualFilter = false;
        } else {
          // Invalid virtual value like "virtual:blah"
          hasInvalidFilter = true;
        }

      } else if (token.contains(":")) {
        // Invalid filter like "Coffee:drink"
        hasInvalidFilter = true;

      } else {
        // Regular search token
        searchTokens.add(token);
      }
    }

    // Join search tokens back into search text
    searchText = String.join(" ", searchTokens).trim();

    // If there's an invalid filter, return empty list
    if (hasInvalidFilter) {
      return new ArrayList<>();
    }

    // Check if search text comes after filters (not allowed)
    if (!searchText.isEmpty() && !cleanedQuery.isEmpty()) {
      int searchTextIndex = cleanedQuery.indexOf(searchText);

      if (locationFilter != null) {
        int locationIndex = cleanedQuery.indexOf("location:");
        if (locationIndex < searchTextIndex) {
          return new ArrayList<>();
        }
      }

      if (virtualFilter != null) {
        int virtualIndex = cleanedQuery.indexOf("virtual:");
        if (virtualIndex < searchTextIndex) {
          return new ArrayList<>();
        }
      }
    }

    // Filter the summaries
    List<Summary> matchingSummaries = new ArrayList<>();

    for (Summary summary : summaries) {
      String summaryTitle = summary.getTitle().toLowerCase();
      String summaryLocation = summary.getLocation().toLowerCase();

      boolean matches = true;

      // Check search text (if any)
      if (!searchText.isEmpty()) {
        // If filters are present, search only in title
        if (locationFilter != null || virtualFilter != null) {
          if (!summaryTitle.contains(searchText)) {
            matches = false;
          }
        } else {
          // No filters, search in both title and location
          if (!summaryTitle.contains(searchText) && !summaryLocation.contains(searchText)) {
            matches = false;
          }
        }
      }

      // Check location filter (if any)
      if (locationFilter != null && !locationFilter.isEmpty()) {
        if (!summaryLocation.contains(locationFilter)) {
          matches = false;
        }
      }

      // Check virtual filter (if any)
      if (virtualFilter != null) {
        if (summary.getVirtual() != virtualFilter) {
          matches = false;
        }
      }

      if (matches) {
        matchingSummaries.add(summary);
      }
    }

    // Sort by time first, then by title (case-insensitive primary, case-sensitive tiebreaker)
    matchingSummaries.sort((s1, s2) -> {
      int timeComparison = s1.start.compareTo(s2.start);
      if (timeComparison != 0) {
        return timeComparison;
      }
      int titleIgnoreCase = s1.title.toLowerCase().compareTo(s2.title.toLowerCase());
      if (titleIgnoreCase != 0) {
        return titleIgnoreCase;
      }
      return s1.title.compareTo(s2.title);
    });

    return matchingSummaries;
  }
}
