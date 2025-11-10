package edu.illinois.cs.cs124.ay2025.mp.activities;

import android.app.Activity;
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
import java.util.Collections;
import java.util.List;

public final class MainActivity extends Activity implements SearchView.OnQueryTextListener {
  // Used for logging messages to help with debugging
  private static final String TAG = MainActivity.class.getSimpleName();

  // Alpha value for inactive (unchecked) buttons
  private static final float BUTTON_ALPHA_INACTIVE = 0.3f;

  // Alpha value for active (checked) buttons
  private static final float BUTTON_ALPHA_ACTIVE = 1.0f;

  // Stores the list of event summaries we get from the server (starts empty)
  private List<Summary> summaries = Collections.emptyList();

  // The adapter connects our data (summaries) to the RecyclerView (the scrollable list)
  private SummaryListAdapter listAdapter;

  // Tracks whether the today filter button is checked (true = show only today's events)
  private boolean isTodayChecked = true;

  // Tracks whether the virtual filter button is checked (true = show only virtual/online events)
  private boolean isVirtualChecked = false;

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
    listAdapter = new SummaryListAdapter(summaries, this);

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
    calendarButton.setChecked(true);
    calendarButton.setAlpha(BUTTON_ALPHA_ACTIVE);
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
  }

  /**
   * onResume is called every time the screen becomes visible to the user. This happens right after
   * onCreate() during startup, and also when returning to this screen. We use this to load fresh
   * data from the server.
   */
  @Override
  protected void onResume() {
    super.onResume(); // Always call parent class first
    loadSummaries(); // Fetch event data from the server
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
                // Extract the list of summaries from the result
                summaries = result.getValue();

                // Switch back to the main UI thread to update the screen
                // (Android requires all UI updates to happen on the main thread)
                runOnUiThread(this::updateDisplayedSummaries);
              } catch (Exception e) {
                // If something goes wrong, log the error for debugging
                Log.e(TAG, "Error updating summary list", e);
              }
            });
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
    List<Summary> displayedSummaries = summaries;

    // Apply today filter if the today button is checked
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
}
