package edu.illinois.cs.cs124.ay2025.mp.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared favorites cache for cross-activity sync. Only ONE instance exists across all activities.
 */
public final class FavoritesRepository {

  private static final Map<String, Boolean> FAVORITES = new HashMap<>();
  private static final List<Runnable> LISTENERS = new ArrayList<>();

  public static synchronized void setFavorite(String id, boolean value) {
    FAVORITES.put(id, value);
    notifyListeners();
  }

  public static synchronized Boolean isFavorite(String id) {
    return FAVORITES.get(id);
  }

  public static synchronized Map<String, Boolean> getAll() {
    return new HashMap<>(FAVORITES); // return a copy
  }

  public static synchronized void clear() {
    FAVORITES.clear();
    notifyListeners();
  }

  public static synchronized void addListener(Runnable listener) {
    LISTENERS.add(listener);
  }

  public static synchronized void removeListener(Runnable listener) {
    LISTENERS.remove(listener);
  }

  private static synchronized void notifyListeners() {
    for (Runnable listener : LISTENERS) {
      listener.run();
    }
  }

  private FavoritesRepository() {
    // Prevent instantiation
  }
}
