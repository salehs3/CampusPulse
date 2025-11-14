package edu.illinois.cs.cs124.ay2025.mp.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.annotation.Nullable;
import edu.illinois.cs.cs124.ay2025.mp.R;
import edu.illinois.cs.cs124.ay2025.mp.application.EventableApplication;
import edu.illinois.cs.cs124.ay2025.mp.models.Event;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * EventActivity - Displays detailed information about a single event.
 *
 * <p>This activity shows all the details for a specific event when the user clicks on an event in
 * the main list. It receives the event ID through an intent extra and loads the full event details
 * from the server.
 */
public final class EventActivity extends Activity {
  // Used for logging messages to help with debugging
  private static final String TAG = EventActivity.class.getSimpleName();

  // Stores the complete event data loaded from the server
  private Event event;

  // Stores the event ID from the intent
  private String eventId;

  /**
   * onCreate is called when the activity is first created. This is where we set up the user
   * interface and load the event details from the server.
   */
  @Override
  protected void onCreate(@Nullable Bundle bundle) {
    super.onCreate(bundle);

    // Load the visual layout from activity_event.xml
    setContentView(R.layout.activity_event);

    // Set the title that appears at the top of the screen
    setTitle("Event Details");

    // Get the event ID from the intent extras
    eventId = getIntent().getStringExtra("id");

    // If no event ID was provided, log an error and return
    if (eventId == null) {
      Log.e(TAG, "No event ID provided in intent");
      return;
    }
  }

  /**
   * onResume is called every time the screen becomes visible to the user. This happens right after
   * onCreate() during startup, and also when returning to this screen.
   */
  @Override
  protected void onResume() {
    super.onResume();
    // Load event data when screen becomes visible
    loadEvent();
  }

  /** Fetches the event details from the server asynchronously and displays them. */
  private void loadEvent() {
    // Don't try to load if we don't have an event ID
    if (eventId == null) {
      return;
    }

    // Get the application object (created at app startup) which has the HTTP client
    EventableApplication application = (EventableApplication) getApplication();

    // Make an async request to get the event details from the server
    application
        .getClient()
        .getEvent(
            eventId,
            (result) -> {
              // This callback runs when the server responds (on a background thread)
              try {
                // Extract the event from the result
                event = result.getValue();

                // Switch back to the main UI thread to update the screen
                // (Android requires all UI updates to happen on the main thread)
                runOnUiThread(this::updateEventDisplay);
              } catch (Exception e) {
                // If something goes wrong, log the error for debugging
                Log.e(TAG, "Error loading event", e);
              }
            });
  }

  /** Updates the UI to display the event details. This must be called on the main UI thread. */
  private void updateEventDisplay() {
    // Don't try to update if event is null
    if (event == null) {
      return;
    }

    // Find the UI elements from the layout
    TextView eventTitleView = findViewById(R.id.event_title);
    TextView eventDetailsView = findViewById(R.id.event_details);
    TextView eventUrlView = findViewById(R.id.event_url);

    // Set the event title
    eventTitleView.setText(event.getTitle());

    // Build the details string with all event information (except URL)
    StringBuilder detailsBuilder = new StringBuilder();

    // Add formatted start time (e.g., "Oct 15 • 3:30 PM")
    try {
      ZonedDateTime startTime = ZonedDateTime.parse(event.getStart());
      String date = startTime.format(DateTimeFormatter.ofPattern("MMM d"));
      String time = startTime.format(DateTimeFormatter.ofPattern("h:mm a"));
      String formattedStart = date + " • " + time;
      detailsBuilder.append(formattedStart).append("\n\n");
    } catch (Exception e) {
      Log.e(TAG, "Error parsing start time", e);
    }

    // Add location if available
    if (!event.getLocation().isBlank()) {
      detailsBuilder.append("Location: ").append(event.getLocation()).append("\n\n");
    }

    // Add description if available
    if (!event.getDescription().isBlank()) {
      detailsBuilder.append(event.getDescription()).append("\n\n");
    }

    // Add virtual indicator if event is virtual
    if (event.isVirtual()) {
      detailsBuilder.append("Virtual Event\n\n");
    }

    // Add categories if available
    if (!event.getCategories().isEmpty()) {
      detailsBuilder.append("Categories: ");
      detailsBuilder.append(String.join(", ", event.getCategories()));
      detailsBuilder.append("\n\n");
    }

    // Add source if available
    if (!event.getSource().isBlank()) {
      detailsBuilder.append("Source: ").append(event.getSource());
    }

    // Set the details text
    eventDetailsView.setText(detailsBuilder.toString().trim());

    // Set the URL in a separate TextView for test matching
    // The test expects to find the URL as exact text in its own TextView
    if (!event.getUrl().isBlank()) {
      eventUrlView.setText(event.getUrl());
      eventUrlView.setVisibility(android.view.View.VISIBLE);
    }
  }
}
