package edu.illinois.cs.cs124.ay2025.mp.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import edu.illinois.cs.cs124.ay2025.mp.R;
import edu.illinois.cs.cs124.ay2025.mp.application.EventableApplication;
import edu.illinois.cs.cs124.ay2025.mp.models.Event;

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

    // TODO: Get event ID from intent extras
    // TODO: Load event details from server
    // TODO: Display event information in the UI
  }

  /**
   * onResume is called every time the screen becomes visible to the user. This happens right after
   * onCreate() during startup, and also when returning to this screen.
   */
  @Override
  protected void onResume() {
    super.onResume();
    // Load event data when screen becomes visible
  }
}
