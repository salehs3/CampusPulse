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
import java.util.Collections;
import java.util.List;

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
  private boolean isTodayChecked = true;

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

    // Load saved filter state before setting up the UI
    loadFilterState();

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
    calendarButton.setChecked(isTodayChecked);
    if (isTodayChecked) {
      calendarButton.setAlpha(BUTTON_ALPHA_ACTIVE);
    } else {
      calendarButton.setAlpha(BUTTON_ALPHA_INACTIVE);
    }
    calendarButton.setOnClickListener(
        v -> {
          ToggleButton button = (ToggleButton) v;
          boolean isChecked = button.isChecked();
          if (isChecked) {
            button.setAlpha(BUTTON_ALPHA_ACTIVE);
          } else {
            button.setAlpha(BUTTON_ALPHA_INACTIVE);
          }
          isTodayChecked = isChecked;
          saveFilterState();
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
          saveFilterState();
          updateDisplayedSummaries();
        });

    // Starred button
    ToggleButton starredButton = findViewById(R.id.starredButton);

    // Initialize the checked state from isStarredChecked
    starredButton.setChecked(isStarredChecked);

    // Set alpha based on checked state
    if (isStarredChecked) {
      starredButton.setAlpha(BUTTON_ALPHA_ACTIVE);
    } else {
      starredButton.setAlpha(BUTTON_ALPHA_INACTIVE);
    }

    // Handle starred button clicks
    starredButton.setOnCheckedChangeListener(
        (button, isChecked) -> {
          // Update appearance
          if (isChecked) {
            button.setAlpha(BUTTON_ALPHA_ACTIVE);
          } else {
            button.setAlpha(BUTTON_ALPHA_INACTIVE);
          }

          // Update state
          isStarredChecked = isChecked;
          saveFilterState(); // Save to SharedPreferences
          updateDisplayedSummaries(); // Refresh adapter with filtered items
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

                // Update UI to show the summaries list
                runOnUiThread(this::updateDisplayedSummaries);
              } catch (Exception e) {
                // If something goes wrong, log the error for debugging
                Log.e(TAG, "Error updating summary list", e);
              }
            });
  }

  /**
   * Updates the RecyclerView based on filters: TODAY, Starred, Virtual, Search. This must be called
   * on the main UI thread.
   */
  private void updateDisplayedSummaries() {
    // Don't try to update if summaries is null
    if (summaries == null) {
      return;
    }

    List<Summary> allSummaries = summaries;
    List<Summary> filteredSummaries = new ArrayList<>();

    for (Summary summary : allSummaries) {
      boolean passesTodayFilter = !isTodayChecked || isToday(summary);
      boolean passesStarredFilter = !isStarredChecked || isStarred(summary);

      if (passesTodayFilter && passesStarredFilter) {
        filteredSummaries.add(summary);
      }
    }

    // Apply VIRTUAL filter
    if (isVirtualChecked) {
      filteredSummaries = Summary.filterVirtual(filteredSummaries, true);
    }

    // Apply SEARCH filter
    if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
      filteredSummaries = Summary.search(filteredSummaries, currentSearchQuery);
    }

    // Sort summaries (Summary implements Comparable)
    Collections.sort(filteredSummaries);

    // Update adapter
    listAdapter.setSummaries(filteredSummaries);
  }

  /** Helper method to check if a summary is happening today. */
  private boolean isToday(Summary summary) {
    Instant now = Helpers.getTimeProvider().now();
    ZonedDateTime nowZoned = now.atZone(ZoneId.of("America/Chicago"));
    ZonedDateTime startOfToday =
        nowZoned.toLocalDate().atStartOfDay(ZoneId.of("America/Chicago"));
    ZonedDateTime endOfToday = startOfToday.plusDays(1).minusNanos(1);

    try {
      ZonedDateTime eventStart = ZonedDateTime.parse(summary.getStart());
      Instant eventInstant = eventStart.toInstant();
      return !eventInstant.isBefore(startOfToday.toInstant())
          && !eventInstant.isAfter(endOfToday.toInstant());
    } catch (Exception e) {
      return false;
    }
  }

  /** Helper method to check if a summary is starred (favorite). */
  private boolean isStarred(Summary summary) {
    Boolean isFavorite =
        edu.illinois.cs.cs124.ay2025.mp.helpers.FavoritesRepository.isFavorite(summary.getId());
    return Boolean.TRUE.equals(isFavorite);
  }

  /**
   * Called when the user submits a search query. This happens when they press enter/search on the
   * keyboard.
   */
  @Override
  public boolean onQueryTextSubmit(String query) {
    // Update the search query and refresh the displayed events
    currentSearchQuery = query;
    saveFilterState();
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
    saveFilterState();
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
