package edu.illinois.cs.cs124.ay2025.mp.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.google.common.truth.Truth.assertWithMessage;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.HTTP.getAPIClient;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.HTTP.testClient;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.TestHelpers.configureLogging;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.TestHelpers.pause;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.TestHelpers.startMainActivity;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.TestHelpers.testSummaryRoute;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.Views.countRecyclerView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import edu.illinois.cs.cs124.ay2025.mp.R;
import edu.illinois.cs.cs124.ay2025.mp.helpers.Helpers;
import edu.illinois.cs.cs124.ay2025.mp.models.Summary;
import edu.illinois.cs.cs124.ay2025.mp.network.Client;
import edu.illinois.cs.cs124.ay2025.mp.network.Server;
import edu.illinois.cs.cs124.ay2025.mp.test.helpers.AdaptiveTimeout;
import edu.illinois.cs.cs124.ay2025.mp.test.helpers.AdaptiveTimeoutRule;
import edu.illinois.cs.cs124.ay2025.mp.test.helpers.Data;
import edu.illinois.cs.cs125.gradlegrader.annotations.Graded;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.robolectric.annotation.LooperMode;
import org.robolectric.annotation.experimental.LazyApplication;

/*
 * This is the MP0 test suite.
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
 * The MP0 test suite is complete and correct. It fully tests all required functionality for this
 * checkpoint. However, in future checkpoints we will begin teaching you how to write your own tests.
 * The test suites we provide will be correct but incomplete, and you'll learn to identify missing
 * test cases and add them yourself as part of the development process.
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
 *
 * Our test suites will sometimes include both graded and ungraded tests.
 * The graded tests are marking with the `@Graded` annotation which includes a point amount.
 * Ungraded tests do not have this annotation.
 * Some ungraded tests will work immediately, and are there to help you pinpoint regressions:
 * changes you made that might have broken things that were previously working.
 * The ungraded tests below were actually written by me (Geoff) during MP development.
 * Other ungraded tests are simply there to help your development process.
 */

@RunWith(AndroidJUnit4.class)
@LooperMode(LooperMode.Mode.PAUSED)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class MP0Test {
  @Rule public AdaptiveTimeoutRule adaptiveTimeout = new AdaptiveTimeoutRule();

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Unit tests that don't require simulating the entire app, and usually complete quickly
  /////////////////////////////////////////////////////////////////////////////////////////////////

  // THIS TEST SHOULD WORK
  @Test
  @AdaptiveTimeout(fast = 1000, slow = 2000)
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test0_SummaryRoute() throws IOException {
    testSummaryRoute();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////
  // Integration tests that require simulating the entire app, and are usually slower
  /////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  @AdaptiveTimeout(fast = 3000, slow = 4000)
  @Graded(points = 90, friendlyName = "Test Main Activity Title")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test1_ActivityTitle() {
    startMainActivity(
        activity -> {
          assertWithMessage("MainActivity has wrong title")
              .that(activity.getTitle())
              .isEqualTo("Discover Events");
        });
  }

  // THIS TEST SHOULD WORK
  @Test
  @AdaptiveTimeout(fast = 1000, slow = 1000)
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test2_ClientGetSummary() throws Exception {
    Client apiClient = getAPIClient();
    List<Summary> summaries = testClient(apiClient::getSummaries);
    assertWithMessage("Summary list is not the right size")
        .that(summaries)
        .hasSize(Data.getSummaryCountFromToday());
  }

  // THIS TEST SHOULD WORK
  @Test
  @AdaptiveTimeout(fast = 1000, slow = 2000)
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test3_ActivitySummaryCount() {
    startMainActivity(
        activity -> {
          pause();
          onView(withId(R.id.todayButton)).perform(click());
          pause();
          onView(withId(R.id.recycler_view))
              .check(countRecyclerView(Data.getSummaryCountFromToday()));
        });
  }

  @BeforeClass
  public static void beforeClass() {
    Helpers.setTimeProvider(() -> Instant.parse("2025-10-15T12:00:00Z"));
    configureLogging();
    Server.start();
  }

  @AfterClass
  public static void afterClass() {
    Server.stop();
  }
}
