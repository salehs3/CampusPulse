package edu.illinois.cs.cs124.ay2025.mp.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.google.common.truth.Truth.assertThat;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.Data.SUMMARIES;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.Data.getShuffledSummaries;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.TestHelpers.configureLogging;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.TestHelpers.pause;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.TestHelpers.startMainActivity;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.Views.countRecyclerView;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.Views.searchFor;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import edu.illinois.cs.cs124.ay2025.mp.R;
import edu.illinois.cs.cs124.ay2025.mp.helpers.Helpers;
import edu.illinois.cs.cs124.ay2025.mp.models.Summary;
import edu.illinois.cs.cs124.ay2025.mp.network.Server;
import edu.illinois.cs.cs124.ay2025.mp.test.helpers.AdaptiveTimeout;
import edu.illinois.cs.cs124.ay2025.mp.test.helpers.AdaptiveTimeoutRule;
import edu.illinois.cs.cs125.gradlegrader.annotations.Graded;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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
 * This is the MP1 test suite.
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
 * The MP1 test suite is correct but incomplete. The tests we provide are accurate, but they don't
 * cover all possible cases. As part of MP1, you'll learn to identify missing test cases and add them
 * yourself. This is an important skill for software development.
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
public final class MP1Test {
  @Rule public AdaptiveTimeoutRule adaptiveTimeout = new AdaptiveTimeoutRule();

  @Test
  @AdaptiveTimeout(fast = 1000, slow = 1000)
  @Graded(points = 10, friendlyName = "Test Summary Sort (Unit)")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test0_testSummarySort() {
    // Add your tests here
    // Test with manually created summaries to verify chronological ordering
    List<Summary> manualTestList = new ArrayList<>();

    // Create summaries with times very close together (1 second apart)
    Summary laterEventByOneSecond = new Summary(
        "event3",
        "Event at 10:00:03",
        "2025-11-03T10:00:03Z",
        "Test Location",
        false
    );

    Summary middleEventByOneSecond = new Summary(
        "event2",
        "Event at 10:00:02",
        "2025-11-03T10:00:02Z",
        "Test Location",
        false
    );

    Summary earlierEventByOneSecond = new Summary(
        "event1",
        "Event at 10:00:01",
        "2025-11-03T10:00:01Z",
        "Test Location",
        false
    );

    // Add them in random order
    manualTestList.add(laterEventByOneSecond);
    manualTestList.add(earlierEventByOneSecond);
    manualTestList.add(middleEventByOneSecond);

    // Sort the list
    Collections.sort(manualTestList);

    // Verify they are now in chronological order (earliest first)
    assertThat(manualTestList.get(0).getId()).isEqualTo("event1");  // 10:00:01
    assertThat(manualTestList.get(1).getId()).isEqualTo("event2");  // 10:00:02
    assertThat(manualTestList.get(2).getId()).isEqualTo("event3");  // 10:00:03

    // Test edge case: Two events with the same start time
    List<Summary> sameDateList = new ArrayList<>();

    Summary firstEventSameTime = new Summary(
        "eventA",
        "First Event",
        "2025-11-20T10:00:00Z",
        "Location A",
        false
    );

    Summary secondEventSameTime = new Summary(
        "eventB",
        "Second Event",
        "2025-11-20T10:00:00Z",
        "Location B",
        true
    );

    Summary differentTimeEvent = new Summary(
        "eventC",
        "Different Time Event",
        "2025-11-20T15:00:00Z",
        "Location C",
        false
    );

    // Add in random order
    sameDateList.add(secondEventSameTime);
    sameDateList.add(differentTimeEvent);
    sameDateList.add(firstEventSameTime);

    Collections.sort(sameDateList);

    // Events with same time should stay stable, but both should come before later event
    assertThat(sameDateList.get(0).getStart()).isEqualTo("2025-11-20T10:00:00Z");
    assertThat(sameDateList.get(1).getStart()).isEqualTo("2025-11-20T10:00:00Z");
    assertThat(sameDateList.get(2).getId()).isEqualTo("eventC");  // Later time comes last

    // Test with provided shuffled summaries
    List<Summary> smallList = getShuffledSummaries(12410, 10);
    Collections.sort(smallList);

    assertThat(smallList.get(0).getId()).isEqualTo("93abbea86c95d909");
    assertThat(smallList.get(1).getId()).isEqualTo("4a729ee96c15cf8b");
    assertThat(smallList.get(2).getId()).isEqualTo("fdd2e81ba0139b3a");
    assertThat(smallList.get(3).getId()).isEqualTo("9bea728583463029");
    assertThat(smallList.get(4).getId()).isEqualTo("b15ac36df9e404fe");
    assertThat(smallList.get(5).getId()).isEqualTo("75ac2c2f0744b14b");
    assertThat(smallList.get(6).getId()).isEqualTo("153fa4eebbc37b3c");
    assertThat(smallList.get(7).getId()).isEqualTo("3aca813563a16a20");
    assertThat(smallList.get(8).getId()).isEqualTo("7d77c89a7bf7e334");
    assertThat(smallList.get(9).getId()).isEqualTo("04b79587f97bcddc");
  }

  @Test
  @AdaptiveTimeout(fast = 1000, slow = 1000)
  @Graded(points = 10, friendlyName = "Test Summary Virtual Filter (Unit)")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test1_testSummaryVirtualFilter() {
    List<Summary> allSummaries = new ArrayList<>(SUMMARIES);

    List<Summary> nonVirtualFiltered = Summary.filterVirtual(allSummaries, false);
    assertThat(nonVirtualFiltered.size()).isEqualTo(2651);
    assertThat(nonVirtualFiltered).isNotSameInstanceAs(allSummaries);
    assertThat(nonVirtualFiltered.get(0).getVirtual()).isFalse();

    // Add your tests here
  }

  @Test
  @AdaptiveTimeout(fast = 1000, slow = 1000)
  @Graded(points = 10, friendlyName = "Test Summary Time Filter (Unit)")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test2_testSummaryTimeFilter() {
    Instant startOfDay = Instant.parse("2025-10-15T05:00:00Z");
    Instant endOfDay = Instant.parse("2025-10-16T04:59:59.999Z");
    List<Summary> todayEvents = Summary.filterTime(SUMMARIES, startOfDay, endOfDay);
    assertThat(todayEvents.size()).isEqualTo(45);
    assertThat(todayEvents).isNotSameInstanceAs(SUMMARIES);

    List<Summary> futureEvents = Summary.filterTime(SUMMARIES, startOfDay, null);
    assertThat(futureEvents.size()).isEqualTo(2349);

    List<Summary> pastEvents = Summary.filterTime(SUMMARIES, null, endOfDay);
    assertThat(pastEvents.size()).isEqualTo(452);

    // Add your tests here
  }

  @Test
  @AdaptiveTimeout(fast = 1000, slow = 1000)
  @Graded(points = 10, friendlyName = "Test Summary Search Basic (Unit)")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test3_testSummarySearchBasic() {
    List<Summary> allEvents = Summary.search(SUMMARIES, "");
    assertThat(allEvents.size()).isEqualTo(2756);
    assertThat(allEvents).isNotSameInstanceAs(SUMMARIES);

    List<Summary> exhibitResults = Summary.search(SUMMARIES, "exhibit");
    assertThat(exhibitResults.size()).isEqualTo(209);
    assertThat(exhibitResults.getFirst().getId()).isEqualTo("9f6535630fbe18ad");
    assertThat(exhibitResults.getLast().getId()).isEqualTo("a536590b0211d019");

    // Add your tests here
  }

  @Test
  @AdaptiveTimeout(fast = 1000, slow = 1000)
  @Graded(points = 20, friendlyName = "Test Summary Search Filters (Unit)")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test4_testSummarySearchFilters() {
    List<Summary> unionResults = Summary.search(SUMMARIES, "location:union");
    assertThat(unionResults.size()).isEqualTo(108);

    List<Summary> virtualResults = Summary.search(SUMMARIES, "virtual:true");
    assertThat(virtualResults.size()).isEqualTo(105);

    List<Summary> coffeeAtUnion = Summary.search(SUMMARIES, "coffee location:union");
    assertThat(coffeeAtUnion.size()).isEqualTo(0);

    // Add your tests here
  }

  @Test
  @AdaptiveTimeout(fast = 1000, slow = 1000)
  @Graded(points = 10, friendlyName = "Test Main Activity Summary Sort (Integration)")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test5_testMainActivitySummarySort() {
    Helpers.setTimeProvider(() -> Instant.parse("2025-10-15T12:00:00Z"));

    startMainActivity(
        activity -> {
          onView(withId(R.id.recycler_view)).check(countRecyclerView(45));

          // Add your tests here
        });
  }

  @Test
  @AdaptiveTimeout(fast = 1000, slow = 1000)
  @Graded(points = 10, friendlyName = "Test Main Activity Search (Integration)")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test6_testMainActivitySearch() {
    Helpers.setTimeProvider(() -> Instant.parse("2025-10-15T12:00:00Z"));

    startMainActivity(
        activity -> {
          onView(withId(R.id.recycler_view)).check(countRecyclerView(45));

          onView(withId(R.id.todayButton)).perform(click());
          pause();
          onView(withId(R.id.recycler_view)).check(countRecyclerView(2349));

          onView(withId(R.id.search)).perform(searchFor("  "));
          pause();
          onView(withId(R.id.recycler_view)).check(countRecyclerView(2349));

          // Add your tests here
        });
  }

  @Test
  @AdaptiveTimeout(fast = 2000, slow = 2000)
  @Graded(points = 10, friendlyName = "Test Main Activity Filter Buttons (Integration)")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test7_testMainActivityFilterButtons() {
    Helpers.setTimeProvider(() -> Instant.parse("2025-10-15T12:00:00Z"));

    startMainActivity(
        activity -> {
          onView(withId(R.id.todayButton)).check(matches(isDisplayed()));
          onView(withId(R.id.virtualButton)).check(matches(isDisplayed()));

          onView(withId(R.id.recycler_view)).check(countRecyclerView(45));

          onView(withId(R.id.todayButton)).perform(click());
          pause();
          onView(withId(R.id.recycler_view)).check(countRecyclerView(2349));

          // Add your tests here
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
