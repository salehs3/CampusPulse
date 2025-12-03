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

  private boolean favorite;

  public boolean isFavorite() {
    return favorite;
  }

  public void setFavorite(boolean setFavorite) {
    this.favorite = setFavorite;
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
    return filteredSummaries;
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
    return filteredSummaries;
  }

  private static final class SearchFilters {
    private String searchText = "";
    private String locationFilter = null;
    private Boolean virtualFilter = null;
    private boolean hasInvalidFilter = false;
  }

  private static SearchFilters parseSearchFilters(String[] tokens) {
    SearchFilters filters = new SearchFilters();
    List<String> searchTokens = new ArrayList<>();

    for (int i = 0; i < tokens.length; i++) {
      String token = tokens[i];

      if (token.startsWith("location:")) {
        filters.locationFilter = extractMultiWordFilter(tokens, i, "location:");
        i = skipFilterTokens(tokens, i);
      } else if (token.startsWith("virtual:")) {
        String virtualValue = token.substring("virtual:".length());
        if (virtualValue.equals("true")) {
          filters.virtualFilter = true;
        } else if (virtualValue.equals("false")) {
          filters.virtualFilter = false;
        } else {
          filters.hasInvalidFilter = true;
        }
      } else {
        searchTokens.add(token);
      }
    }

    filters.searchText = String.join(" ", searchTokens).trim();
    return filters;
  }

  private static String extractMultiWordFilter(String[] tokens, int startIndex, String prefix) {
    String firstValue = tokens[startIndex].substring(prefix.length());
    List<String> parts = new ArrayList<>();
    if (!firstValue.isEmpty()) {
      parts.add(firstValue);
    }

    for (int j = startIndex + 1; j < tokens.length; j++) {
      if (tokens[j].startsWith("location:")
          || tokens[j].startsWith("virtual:")
          || tokens[j].contains(":")) {
        break;
      }
      parts.add(tokens[j]);
    }

    return String.join(" ", parts).trim();
  }

  private static int skipFilterTokens(String[] tokens, int startIndex) {
    int i = startIndex;
    for (int j = startIndex + 1; j < tokens.length; j++) {
      if (tokens[j].startsWith("location:")
          || tokens[j].startsWith("virtual:")
          || tokens[j].contains(":")) {
        break;
      }
      i = j;
    }
    return i;
  }

  private static boolean isSearchTextBeforeFilters(
      String cleanedQuery, String searchText, String locationFilter, Boolean virtualFilter) {
    if (searchText.isEmpty() || cleanedQuery.isEmpty()) {
      return true;
    }

    int searchTextIndex = cleanedQuery.indexOf(searchText);

    if (locationFilter != null) {
      int locationIndex = cleanedQuery.indexOf("location:");
      if (locationIndex < searchTextIndex) {
        return false;
      }
    }

    if (virtualFilter != null) {
      int virtualIndex = cleanedQuery.indexOf("virtual:");
      if (virtualIndex < searchTextIndex) {
        return false;
      }
    }

    return true;
  }

  private static boolean matchesSummary(
      Summary summary, String searchText, String locationFilter, Boolean virtualFilter) {
    String summaryTitle = summary.getTitle().toLowerCase();
    String summaryLocation = summary.getLocation().toLowerCase();

    if (!searchText.isEmpty()) {
      if (locationFilter != null || virtualFilter != null) {
        if (!summaryTitle.contains(searchText)) {
          return false;
        }
      } else {
        if (!summaryTitle.contains(searchText) && !summaryLocation.contains(searchText)) {
          return false;
        }
      }
    }

    if (locationFilter != null && !locationFilter.isEmpty()) {
      if (!summaryLocation.contains(locationFilter)) {
        return false;
      }
    }

    if (virtualFilter != null) {
      if (summary.getVirtual() != virtualFilter) {
        return false;
      }
    }

    return true;
  }

  public static List<Summary> search(List<Summary> summaries, String query) {
    String cleanedQuery = query.trim().toLowerCase();

    if (cleanedQuery.isEmpty()) {
      List<Summary> allSummaries = new ArrayList<>(summaries);
      Collections.sort(allSummaries);
      return allSummaries;
    }

    String[] tokens = cleanedQuery.split("\\s+");
    SearchFilters filters = parseSearchFilters(tokens);

    if (filters.hasInvalidFilter) {
      return new ArrayList<>();
    }

    if (!isSearchTextBeforeFilters(
        cleanedQuery, filters.searchText, filters.locationFilter, filters.virtualFilter)) {
      return new ArrayList<>();
    }

    List<Summary> matchingSummaries = new ArrayList<>();
    for (Summary summary : summaries) {
      if (matchesSummary(
          summary, filters.searchText, filters.locationFilter, filters.virtualFilter)) {
        matchingSummaries.add(summary);
      }
    }

    Collections.sort(matchingSummaries);

    return matchingSummaries;
  }
}
