package edu.illinois.cs.cs124.ay2025.mp.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.google.common.truth.Truth.assertThat;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.Data.EVENT_DATA;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.Data.SUMMARIES;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.Data.getShuffledSummaries;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.Data.getSummaryCountToday;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.HTTP.getAPIClient;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.HTTP.testClient;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.HTTP.testServerGet;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.RecyclerViewMatcher.withRecyclerView;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.TestHelpers.checkServerDesign;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.TestHelpers.configureLogging;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.TestHelpers.pause;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.TestHelpers.startActivity;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.TestHelpers.startMainActivity;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.TestHelpers.testSummaryRoute;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.Views.countRecyclerView;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import edu.illinois.cs.cs124.ay2025.mp.R;
import edu.illinois.cs.cs124.ay2025.mp.activities.EventActivity;
import edu.illinois.cs.cs124.ay2025.mp.helpers.Helpers;
import edu.illinois.cs.cs124.ay2025.mp.models.Event;
import edu.illinois.cs.cs124.ay2025.mp.models.Summary;
import edu.illinois.cs.cs124.ay2025.mp.network.Client;
import edu.illinois.cs.cs124.ay2025.mp.network.Server;
import edu.illinois.cs.cs124.ay2025.mp.test.helpers.AdaptiveTimeout;
import edu.illinois.cs.cs124.ay2025.mp.test.helpers.AdaptiveTimeoutRule;
import edu.illinois.cs.cs124.ay2025.mp.test.helpers.JSONReadCountSecurityManager;
import edu.illinois.cs.cs125.gradlegrader.annotations.Graded;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.robolectric.annotation.LooperMode;
import org.robolectric.annotation.experimental.LazyApplication;

/*
 * This is the MP2 test suite.
 * The code below is used to evaluate your app during testing, local grading, and official grading.
 * You may not understand all of the code below, but you'll need to have some understanding of how
 * it works so that you can determine what is wrong with your app and what you need to fix.
 *
 * ALL CHANGES TO THIS FILE WILL BE OVERWRITTEN DURING OFFICIAL GRADING.
 * You can and should modify the code below if it is useful during your own local testing,
 * but any changes you make will be discarded during official grading.
 * The local grader will not run if the test suites have been modified, so you'll need to undo any
 * local changes before you run the grader.
 *
 * The MP2 test suite is correct but incomplete. The tests we provide are accurate, but they don't
 * cover all possible cases. As part of MP2, you'll continue learning to identify missing test cases
 * and add them yourself. This is an important skill for software development.
 *
 * Note that when you add your own tests or test cases, those tests might be incorrect. We'll help
 * you learn to write good tests, but it's important to understand that a failing test you wrote
 * might indicate a problem with the test, not with your app.
 *
 * Our test suites are broken into two parts.
 * Unit tests can complete without running your app.
 * They test things like whether a method works properly or the behavior of your API server.
 * Unit tests are usually fairly fast.
 *
 * Integration tests require simulating your app.
 * This allows us to test things like your API client, and higher-level aspects of your app's
 * behavior, such as whether it displays the right thing on the display.
 * Because integration tests require simulating your entire app, they run more slowly.
 */

@RunWith(AndroidJUnit4.class)
@LooperMode(LooperMode.Mode.PAUSED)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class MP2Test {
  @Rule public AdaptiveTimeoutRule adaptiveTimeout = new AdaptiveTimeoutRule();

  @Test
  @AdaptiveTimeout(fast = 1000, slow = 1000)
  @Graded(points = 20, friendlyName = "Server GET /event (Unit)")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test0_ServerEventRoute() throws IOException {
    testSummaryRoute();

    List<Summary> trimmedSummaries = getShuffledSummaries(12421, 128);

    for (Summary eventSummary : trimmedSummaries) {
      Event event = testServerGet("/event/" + eventSummary.getId(), Event.class);

      // Add your tests here
    }

    testServerGet("/events/61801f5ee9ce3704", HttpURLConnection.HTTP_NOT_FOUND);
    testServerGet("/event/61801f5ee9ce3705", HttpURLConnection.HTTP_NOT_FOUND);
    testServerGet("/event/", HttpURLConnection.HTTP_NOT_FOUND);
  }

  @Test
  @AdaptiveTimeout(fast = 1000, slow = 1000)
  @Graded(points = 20, friendlyName = "Client getEvent (Integration)")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test1_ClientGetEvent() throws Exception {
    Client apiClient = getAPIClient();

    List<Summary> trimmedSummaries = getShuffledSummaries(12422, 32);

    for (Summary summary : trimmedSummaries) {
      Event event = testClient((callback) -> apiClient.getEvent(summary.getId(), callback));

      // Add your tests here
    }

    try {
      Event ignored = testClient((callback) -> apiClient.getEvent("61801f5ee9ce3705", callback));
      fail("Client getEvent for non-existent event should throw");
    } catch (Exception ignored) {
    }
  }

  @SuppressWarnings("SpellCheckingInspection")
  @Test
  @AdaptiveTimeout(fast = 1000, slow = 1000)
  @Graded(points = 20, friendlyName = "Summary Click Launch (Integration)")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test2_SummaryClickLaunch() {
    startMainActivity(
        activity -> {
          onView(withId(R.id.recycler_view)).check(countRecyclerView(getSummaryCountToday()));

          onView(withRecyclerView(R.id.recycler_view).atPosition(2))
              .check(matches(hasDescendant(withText("Freestyle"))));

          onView(withRecyclerView(R.id.recycler_view).atPosition(2)).perform(click());

          String id = shadowOf(activity).getNextStartedActivity().getStringExtra("id");
          assertThat(id).isEqualTo("bc1dcfbdef502f70");
        });
  }

  @Test
  @AdaptiveTimeout(fast = 2000, slow = 2000)
  @Graded(points = 20, friendlyName = "Event View (Integration)")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test3_EventView() throws Exception {
    Client apiClient = getAPIClient();

    List<Event> completeEvents = new ArrayList<>();
    for (Summary summary : SUMMARIES) {
      Event event = testClient((callback) -> apiClient.getEvent(summary.getId(), callback));

      if (!event.getUrl().isBlank()
          && !event.getDescription().isBlank()
          && !event.getCategories().isEmpty()
          && !event.getLocation().isBlank()
          && !event.getSource().isBlank()) {
        completeEvents.add(event);
        if (completeEvents.size() >= 32) {
          break;
        }
      }
    }

    assertThat(completeEvents.size()).isAtLeast(4);

    Collections.shuffle(completeEvents, new Random(12424));
    List<Event> eventsToTest = completeEvents.subList(0, Math.min(4, completeEvents.size()));

    for (Event event : eventsToTest) {
      Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventActivity.class);
      intent.putExtra("id", event.getId());

      startActivity(
          intent,
          activity -> {
            pause();

            // Add your tests here

            try {
              ZonedDateTime startTime = ZonedDateTime.parse(event.getStart());
              String date = startTime.format(DateTimeFormatter.ofPattern("MMM d"));
              String time = startTime.format(DateTimeFormatter.ofPattern("h:mm a"));
              String formattedStart = date + " • " + time;
              onView(first(withText(containsString(formattedStart)))).check(matches(isDisplayed()));
            } catch (Exception e) {
              fail("Failed to parse or verify start time");
            }
          });
    }
  }

  private static final JSONReadCountSecurityManager JSON_READ_COUNT_SECURITY_MANAGER =
      new JSONReadCountSecurityManager();

  @BeforeClass
  public static void beforeClass() {
    Helpers.setTimeProvider(() -> Instant.parse("2025-10-15T12:00:00Z"));

    checkServerDesign();

    configureLogging();

    EVENT_DATA.size();
    SUMMARIES.size();

    System.setSecurityManager(JSON_READ_COUNT_SECURITY_MANAGER);

    Server.start();
  }

  @AfterClass
  public static void afterClass() {
    Server.stop();

    System.setSecurityManager(null);
  }

  @Before
  public void beforeTest() {
    JSON_READ_COUNT_SECURITY_MANAGER.checkCount();
  }

  @After
  public void afterTest() {
    JSON_READ_COUNT_SECURITY_MANAGER.checkCount();
  }

  private <T> Matcher<T> first(final Matcher<T> matcher) {
    return new BaseMatcher<>() {
      boolean isFirst = true;

      @Override
      public boolean matches(final Object item) {
        if (isFirst && matcher.matches(item)) {
          isFirst = false;
          return true;
        }
        return false;
      }

      @Override
      public void describeTo(final Description description) {
        description.appendText("should return first matching item");
      }
    };
  }
}
