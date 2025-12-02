package edu.illinois.cs.cs124.ay2025.mp.models;

import androidx.annotation.NonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Objects;

/**
 * Favorite - Represents the favorite status of an event.
 *
 * <p>This class stores whether a particular event (identified by its ID) has been marked as a
 * favorite by the user.
 */
public final class Favorite {
  @NonNull private final String id;
  private final boolean favorite;

  /**
   * Constructor for creating a Favorite object.
   *
   * @param setId The unique identifier of the event
   * @param setFavorite Whether the event is marked as favorite (true) or not (false)
   */
  @JsonCreator
  public Favorite(@NonNull String setId, boolean setFavorite) {
    id = setId;
    favorite = setFavorite;
  }

  @NonNull
  public String getId() {
    return id;
  }

  public boolean getFavorite() {
    return favorite;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Favorite other)) {
      return false;
    }
    return Objects.equals(id, other.id) && favorite == other.favorite;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, favorite);
  }
}
