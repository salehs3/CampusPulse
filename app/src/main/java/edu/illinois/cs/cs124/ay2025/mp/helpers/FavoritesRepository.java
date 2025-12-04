package edu.illinois.cs.cs124.ay2025.mp.helpers;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared favorites cache for cross-activity sync. Only ONE instance exists across all activities.
 */
public final class FavoritesRepository {

  private static final Map<String, Boolean> FAVORITES = new HashMap<>();

  public static synchronized void setFavorite(String id, boolean value) {
    FAVORITES.put(id, value);
  }

  public static synchronized Boolean isFavorite(String id) {
    return FAVORITES.get(id);
  }

  public static synchronized Map<String, Boolean> getAll() {
    return new HashMap<>(FAVORITES); // return a copy
  }

  public static synchronized void clear() {
    FAVORITES.clear();
  }

  private FavoritesRepository() {
    // Prevent instantiation
  }
}
