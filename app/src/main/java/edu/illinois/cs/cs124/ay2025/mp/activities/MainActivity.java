package edu.illinois.cs.cs124.ay2025.mp.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Insets;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowInsets;
import android.widget.SearchView;
import android.widget.ToggleButton;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import edu.illinois.cs.cs124.ay2025.mp.R;
import edu.illinois.cs.cs124.ay2025.mp.adapters.SummaryListAdapter;
import edu.illinois.cs.cs124.ay2025.mp.application.EventableApplication;
import edu.illinois.cs.cs124.ay2025.mp.helpers.Helpers;
import edu.illinois.cs.cs124.ay2025.mp.models.Summary;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class MainActivity extends Activity implements SearchView.OnQueryTextListener {
  // Used for logging messages to help with debugging
  private static final String TAG = MainActivity.class.getSimpleName();

  // Alpha value for inactive (unchecked) buttons
  private static final float BUTTON_ALPHA_INACTIVE = 0.3f;

  // Alpha value for active (checked) buttons
  private static final float BUTTON_ALPHA_ACTIVE = 1.0f;

  // SharedPreferences keys for persisting filter state
  private static final String PREF_KEY_TODAY_CHECKED = "filter_today_checked";
  private static final String PREF_KEY_VIRTUAL_CHECKED = "filter_virtual_checked";
  private static final String PREF_KEY_SEARCH_QUERY = "filter_search_query";

  // Stores the list of event summaries we get from the server (starts null until loaded)
  private List<Summary> summaries = null;

  // The adapter connects our data (summaries) to the RecyclerView (the scrollable list)
  private SummaryListAdapter listAdapter;

  // Tracks whether the today filter button is checked (true = show only today's events)
  private boolean isTodayChecked = false;

  // Tracks whether the virtual filter button is checked (true = show only virtual/online events)
  private boolean isVirtualChecked = false;

  // Tracks whether the starred filter button is checked (true = show only starred/favorite events)
  private boolean isStarredChecked = false;

  // Stores the current search query text (empty string means no search filter)
  private String currentSearchQuery = "";

  /**
   * onCreate is called ONCE when the activity is first created (part of the startup process). This
   * is where we set up the user interface and prepare everything for display.
   */
  @Override
  protected void onCreate(@Nullable Bundle bundle) {
    super.onCreate(bundle); // Always call parent class first

    // Load the visual layout from activity_main.xml
    setContentView(R.layout.activity_main);

    // Set the title that appears at the top of the screen
    setTitle("Discover Events");

    // Create the adapter (starts with empty list, will update later when data loads)
    // Pass a click callback that launches EventActivity when a summary is clicked
    listAdapter =
        new SummaryListAdapter(
            new ArrayList<>(),
            this,
            (clickedSummary) -> {
              // Create an intent to launch EventActivity
              Intent eventIntent = new Intent(this, EventActivity.class);
              // Add the event ID as an extra so EventActivity knows which event to load
              eventIntent.putExtra("id", clickedSummary.getId());
              // Start the EventActivity
              startActivity(eventIntent);
            });

    // Find the RecyclerView (scrollable list) from the layout and configure it
    RecyclerView recyclerView = findViewById(R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(this)); // Make it scroll vertically
    recyclerView.setAdapter(listAdapter); // Connect the adapter to show our data

    // Set up the toolbar (the bar at the top with search and filters)
    setActionBar(findViewById(R.id.toolbar));

    // Handle window insets (makes sure our content doesn't hide behind status bar/navigation bar)
    findViewById(R.id.container)
        .setOnApplyWindowInsetsListener(
            (v, windowInsets) -> {
              Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars());
              v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
              return WindowInsets.CONSUMED;
            });

    // Set up the search view with query text listener
    SearchView searchView = findViewById(R.id.search);
    searchView.setOnQueryTextListener(this);

    // Set up the calendar button (today filter) click handler
    ToggleButton calendarButton = findViewById(R.id.todayButton);
    calendarButton.setChecked(false);
    calendarButton.setAlpha(BUTTON_ALPHA_INACTIVE);
    calendarButton.setOnClickListener(
        (v) -> {
          // Handle calendar button click
          ToggleButton button = (ToggleButton) v;
          boolean isChecked = button.isChecked();

          // Update button appearance based on checked state
          if (isChecked) {
            button.setAlpha(BUTTON_ALPHA_ACTIVE);
          } else {
            button.setAlpha(BUTTON_ALPHA_INACTIVE);
          }

          // Log the button state for debugging
          Log.d(TAG, "Calendar button clicked. Showing today's events: " + isChecked);

          // Update the today filter state and refresh the displayed events
          isTodayChecked = isChecked;
          updateDisplayedSummaries();
        });

    // Set up the virtual button (virtual/online events filter) click handler
    ToggleButton virtualButton = findViewById(R.id.virtualButton);
    virtualButton.setOnClickListener(
        (v) -> {
          // Handle virtual button click
          ToggleButton button = (ToggleButton) v;
          boolean isChecked = button.isChecked();

          // Update button appearance based on checked state
          if (isChecked) {
            button.setAlpha(BUTTON_ALPHA_ACTIVE);
          } else {
            button.setAlpha(BUTTON_ALPHA_INACTIVE);
          }

          // Log the button state for debugging
          Log.d(TAG, "Virtual button clicked. Showing virtual events: " + isChecked);

          // Update the virtual filter state and refresh the displayed events
          isVirtualChecked = isChecked;
          updateDisplayedSummaries();
        });

    // Set up the starred button (starred/favorite events filter) click handler
    // Initialize button state before setting listener to prevent spurious events
    ToggleButton starredButton = findViewById(R.id.starredButton);
    starredButton.setChecked(false);
    starredButton.setAlpha(BUTTON_ALPHA_INACTIVE);

    // Using setOnClickListener because tests trigger clicks, not state changes
    starredButton.setOnClickListener(
        (v) -> {
          // Handle starred button click
          ToggleButton button = (ToggleButton) v;
          boolean isChecked = button.isChecked();

          // Update the starred filter state
          isStarredChecked = isChecked;

          // Update button appearance based on checked state
          if (isChecked) {
            button.setAlpha(BUTTON_ALPHA_ACTIVE);
          } else {
            button.setAlpha(BUTTON_ALPHA_INACTIVE);
          }

          // Log the button state for debugging
          Log.d(TAG, "Starred button clicked. Showing starred events: " + isChecked);

          // Apply the starred filter
          applyStarredFilter();
        });

    // Load initial data from server
    loadSummaries();
  }

  /**
   * onResume is called every time the screen becomes visible to the user. This happens right after
   * onCreate() during startup, and also when returning to this screen. We use this to load fresh
   * data from the server.
   */
  @Override
  protected void onResume() {
    super.onResume(); // Always call parent class first

    // If summaries is null, it means onCreate hasn't finished its initial load yet.
    // Let onCreate handle it to avoid a race condition.
    if (summaries == null) {
      return;
    }

    // Re-fetch the data from the server
    loadSummaries();
  }

  /**
   * Fetches the list of event summaries from the server asynchronously. This happens in the
   * background so the UI doesn't freeze while waiting for the server.
   */
  private void loadSummaries() {
    // Get the application object (created at app startup) which has the HTTP client
    EventableApplication application = (EventableApplication) getApplication();

    // Make an async request to get summaries from the server
    application
        .getClient()
        .getSummaries(
            (result) -> {
              // This callback runs when the server responds (on a background thread)
              try {
                // Store the full list of summaries
                summaries = result.getValue();

                // Load favorite status for ALL summaries to populate the cache
                // This ensures the starred filter works even if favorites were set via API
                EventableApplication app = (EventableApplication) getApplication();

                // Create atomic counter to track completion of all favorite loads
                AtomicInteger pendingLoads = new AtomicInteger(summaries.size());

                // Load favorite status for each summary
                for (Summary summary : summaries) {
                  String eventId = summary.getId();

                  app.getClient()
                      .getFavorite(
                          eventId,
                          (favResult) -> {
                            try {
                              boolean isFav = favResult.getValue();
                              summary.setFavorite(isFav);
                              app.updateFavoriteCache(eventId, isFav);
                            } catch (Exception ex) {
                              Log.d(TAG, "Could not load favorite for " + eventId);
                            } finally {
                              // Decrement counter and update UI when all complete
                              if (pendingLoads.decrementAndGet() == 0) {
                                // All favorites loaded, now update UI on main thread
                                runOnUiThread(this::updateDisplayedSummaries);
                              }
                            }
                          });
                }
              } catch (Exception e) {
                // If something goes wrong, log the error for debugging
                Log.e(TAG, "Error updating summary list", e);
              }
            });
  }

  /**
   * Applies the starred filter by loading favorites first if needed, then updating the display.
   */
  private void applyStarredFilter() {
    if (isStarredChecked) {
      // Load favorites for current summaries first, then update display
      loadFavoritesThenFilter();
    } else {
      // Show all if filter is off
      updateDisplayedSummaries();
    }
  }

  /**
   * Loads favorite status for all current summaries from the server, then applies the filter. This
   * is called when the starred filter is turned ON to ensure we have the favorite data needed for
   * filtering.
   */
  private void loadFavoritesThenFilter() {
    if (summaries == null || summaries.isEmpty()) {
      updateDisplayedSummaries();
      return;
    }

    // Get the application to access the client and cache
    EventableApplication application = (EventableApplication) getApplication();

    // Create array to track completion of all favorite loads
    AtomicInteger pendingLoads = new AtomicInteger(summaries.size());

    // Load favorite status for each summary
    for (Summary summary : summaries) {
      String eventId = summary.getId();

      application
          .getClient()
          .getFavorite(
              eventId,
              (result) -> {
                try {
                  // Update the cache with the favorite status from the server
                  boolean isFavorite = result.getValue();
                  summary.setFavorite(isFavorite);
                  application.updateFavoriteCache(eventId, isFavorite);
                } catch (Exception e) {
                  // If request fails, just log it
                  Log.d(TAG, "Could not load favorite for " + eventId);
                } finally {
                  // Decrement counter and update UI when all complete
                  if (pendingLoads.decrementAndGet() == 0) {
                    // All favorites loaded, now update UI on main thread
                    runOnUiThread(this::updateDisplayedSummaries);
                  }
                }
              });
    }
  }

  /**
   * Updates the RecyclerView to display the summaries we fetched from the server. This must be
   * called on the main UI thread.
   */
  private void updateDisplayedSummaries() {
    // Don't try to update if summaries is null
    if (summaries == null) {
      return;
    }

    // Start with all summaries from the server
    List<Summary> displayedSummaries = new ArrayList<>(summaries);

    // Apply today filter if the today button is checked (applies to both starred and non-starred)
    if (isTodayChecked) {
      // Get the current time using the time provider (not Instant.now() directly)
      Instant currentTime = Helpers.getTimeProvider().now();

      // Convert to America/Chicago timezone to get today's date
      ZonedDateTime currentChicagoTime = currentTime.atZone(ZoneId.of("America/Chicago"));

      // Calculate start of today (midnight) in America/Chicago timezone
      ZonedDateTime startOfToday =
          currentChicagoTime.toLocalDate().atStartOfDay(ZoneId.of("America/Chicago"));
      Instant todayStart = startOfToday.toInstant();

      // Calculate end of today (one nanosecond before midnight tomorrow)
      ZonedDateTime startOfTomorrow = startOfToday.plusDays(1);
      Instant todayEnd = startOfTomorrow.toInstant().minusNanos(1);

      // Use Summary.filterTime to get only today's events
      displayedSummaries = Summary.filterTime(displayedSummaries, todayStart, todayEnd);
    }

    // Apply virtual filter if the virtual button is checked
    if (isVirtualChecked) {
      // Use Summary.filterVirtual to get only virtual/online events
      displayedSummaries = Summary.filterVirtual(displayedSummaries, true);
    }

    // Apply search filter if there is a search query
    displayedSummaries = Summary.search(displayedSummaries, currentSearchQuery);

    // Apply starred filter if the starred button is checked
    if (isStarredChecked) {
      // Filter to show only events that are marked as favorites
      List<Summary> starredSummaries = new ArrayList<>();
      for (Summary summary : displayedSummaries) {
        if (summary.isFavorite()) {
          starredSummaries.add(summary);
        }
      }
      displayedSummaries = starredSummaries;
    }

    // Tell the adapter about the new data, which triggers the RecyclerView to refresh
    // This runs even if the list is empty, so the adapter knows there are no events to display
    listAdapter.setSummaries(displayedSummaries);
  }

  /**
   * Called when the user submits a search query. This happens when they press enter/search on the
   * keyboard.
   */
  @Override
  public boolean onQueryTextSubmit(String query) {
    // Update the search query and refresh the displayed events
    currentSearchQuery = query;
    updateDisplayedSummaries();
    return true;
  }

  /**
   * Called when the search query text changes. This happens as the user types in the search box.
   */
  @Override
  public boolean onQueryTextChange(String newText) {
    // Update the search query and refresh the displayed events in real-time
    currentSearchQuery = newText;
    updateDisplayedSummaries();
    return true;
  }

  /**
   * Loads saved filter state from SharedPreferences. Should be called early in onCreate() before UI
   * initialization.
   */
  private void loadFilterState() {
    SharedPreferences preferences = getPreferences(MODE_PRIVATE);
    isTodayChecked = preferences.getBoolean(PREF_KEY_TODAY_CHECKED, true);
    isVirtualChecked = preferences.getBoolean(PREF_KEY_VIRTUAL_CHECKED, false);
    currentSearchQuery = preferences.getString(PREF_KEY_SEARCH_QUERY, "");
    Log.d(
        TAG,
        "Loaded filter state - Today: "
            + isTodayChecked
            + ", Virtual: "
            + isVirtualChecked
            + ", Search: '"
            + currentSearchQuery
            + "'");
  }

  /**
   * Saves current filter state to SharedPreferences. Should be called whenever any filter value
   * changes.
   */
  private void saveFilterState() {
    SharedPreferences preferences = getPreferences(MODE_PRIVATE);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putBoolean(PREF_KEY_TODAY_CHECKED, isTodayChecked);
    editor.putBoolean(PREF_KEY_VIRTUAL_CHECKED, isVirtualChecked);
    editor.putString(PREF_KEY_SEARCH_QUERY, currentSearchQuery);
    editor.apply();
    Log.d(
        TAG,
        "Saved filter state - Today: "
            + isTodayChecked
            + ", Virtual: "
            + isVirtualChecked
            + ", Search: '"
            + currentSearchQuery
            + "'");
  }
}
