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
    Summary laterEventByOneSecond =
        new Summary("event3", "Event at 10:00:03", "2025-11-03T10:00:03Z", "Test Location", false);

    Summary middleEventByOneSecond =
        new Summary("event2", "Event at 10:00:02", "2025-11-03T10:00:02Z", "Test Location", false);

    Summary earlierEventByOneSecond =
        new Summary("event1", "Event at 10:00:01", "2025-11-03T10:00:01Z", "Test Location", false);

    // Add them in random order
    manualTestList.add(laterEventByOneSecond);
    manualTestList.add(earlierEventByOneSecond);
    manualTestList.add(middleEventByOneSecond);

    // Sort the list
    Collections.sort(manualTestList);

    // Verify they are now in chronological order (earliest first)
    assertThat(manualTestList.get(0).getId()).isEqualTo("event1"); // 10:00:01
    assertThat(manualTestList.get(1).getId()).isEqualTo("event2"); // 10:00:02
    assertThat(manualTestList.get(2).getId()).isEqualTo("event3"); // 10:00:03

    // Test edge case: Two events with the same start time
    List<Summary> sameDateList = new ArrayList<>();

    Summary firstEventSameTime =
        new Summary("eventA", "First Event", "2025-11-20T10:00:00Z", "Location A", false);

    Summary secondEventSameTime =
        new Summary("eventB", "Second Event", "2025-11-20T10:00:00Z", "Location B", true);

    Summary differentTimeEvent =
        new Summary("eventC", "Different Time Event", "2025-11-20T15:00:00Z", "Location C", false);

    // Add in random order
    sameDateList.add(secondEventSameTime);
    sameDateList.add(differentTimeEvent);
    sameDateList.add(firstEventSameTime);

    Collections.sort(sameDateList);

    // Events with same time should stay stable, but both should come before later event
    assertThat(sameDateList.get(0).getStart()).isEqualTo("2025-11-20T10:00:00Z");
    assertThat(sameDateList.get(1).getStart()).isEqualTo("2025-11-20T10:00:00Z");
    assertThat(sameDateList.get(2).getId()).isEqualTo("eventC"); // Later time comes last

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
    // Test filtering for virtual events (virtual=true)
    List<Summary> virtualFiltered = Summary.filterVirtual(allSummaries, true);
    assertThat(virtualFiltered.size()).isEqualTo(105);
    assertThat(virtualFiltered).isNotSameInstanceAs(allSummaries);
    assertThat(virtualFiltered.get(0).getVirtual()).isTrue();

    // Test with manually created mock data
    List<Summary> mockSummaries = new ArrayList<>();

    // Create mock virtual events
    Summary virtualEvent1 =
        new Summary("virtual1", "Online Workshop", "2025-11-05T14:00:00Z", "Zoom", true);

    Summary virtualEvent2 =
        new Summary(
            "virtual2", "Virtual Conference", "2025-11-06T10:00:00Z", "Microsoft Teams", true);

    // Create mock in-person events
    Summary inPersonEvent1 =
        new Summary("inperson1", "Campus Lecture", "2025-11-07T15:00:00Z", "Siebel Center", false);

    Summary inPersonEvent2 =
        new Summary(
            "inperson2", "Study Session", "2025-11-08T18:00:00Z", "Grainger Library", false);

    // Add all events to the list
    mockSummaries.add(virtualEvent1);
    mockSummaries.add(inPersonEvent1);
    mockSummaries.add(virtualEvent2);
    mockSummaries.add(inPersonEvent2);

    // Filter for virtual events only
    List<Summary> mockVirtualFiltered = Summary.filterVirtual(mockSummaries, true);
    assertThat(mockVirtualFiltered.size()).isEqualTo(2);
    for (Summary eventSummary : mockVirtualFiltered) {
      assertThat(eventSummary.getVirtual()).isTrue();
    }

    // Filter for in-person events only
    List<Summary> mockInPersonFiltered = Summary.filterVirtual(mockSummaries, false);
    assertThat(mockInPersonFiltered.size()).isEqualTo(2);
    for (Summary eventSummary : mockInPersonFiltered) {
      assertThat(eventSummary.getVirtual()).isFalse();
    }

    // Test with empty list
    List<Summary> emptySummaries = new ArrayList<>();
    List<Summary> emptyVirtualFiltered = Summary.filterVirtual(emptySummaries, true);
    assertThat(emptyVirtualFiltered.size()).isEqualTo(0);

    List<Summary> emptyInPersonFiltered = Summary.filterVirtual(emptySummaries, false);
    assertThat(emptyInPersonFiltered.size()).isEqualTo(0);
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
    // Test with manually created mock data
    List<Summary> mockTimeFilterSummaries = new ArrayList<>();

    // Create events at different times
    Summary earlyMorningEvent =
        new Summary("early1", "Early Morning Event", "2025-11-10T06:00:00Z", "Coffee Shop", false);

    Summary midDayEvent =
        new Summary("midday1", "Lunch Meeting", "2025-11-10T17:00:00Z", "Restaurant", false);

    Summary eveningEvent =
        new Summary("evening1", "Evening Concert", "2025-11-10T23:00:00Z", "Theater", false);

    Summary nextDayEvent =
        new Summary("nextday1", "Next Day Event", "2025-11-11T10:00:00Z", "Campus Center", false);

    Summary previousDayEvent =
        new Summary("prevday1", "Previous Day Event", "2025-11-09T14:00:00Z", "Library", true);

    // Add all events to the list
    mockTimeFilterSummaries.add(earlyMorningEvent);
    mockTimeFilterSummaries.add(midDayEvent);
    mockTimeFilterSummaries.add(eveningEvent);
    mockTimeFilterSummaries.add(nextDayEvent);
    mockTimeFilterSummaries.add(previousDayEvent);

    // Test filtering for events on November 10, 2025
    Instant nov10Start = Instant.parse("2025-11-10T00:00:00Z");
    Instant nov10End = Instant.parse("2025-11-10T23:59:59.999Z");
    List<Summary> nov10Events = Summary.filterTime(mockTimeFilterSummaries, nov10Start, nov10End);
    assertThat(nov10Events.size()).isEqualTo(3);
    assertThat(nov10Events.get(0).getId()).isEqualTo("early1");
    assertThat(nov10Events.get(1).getId()).isEqualTo("midday1");
    assertThat(nov10Events.get(2).getId()).isEqualTo("evening1");

    // Test filtering for events on or after November 10, 2025 (future events)
    List<Summary> mockFutureEvents = Summary.filterTime(mockTimeFilterSummaries, nov10Start, null);
    assertThat(mockFutureEvents.size()).isEqualTo(4);

    // Test filtering for events before or on November 10, 2025 (past events)
    List<Summary> mockPastEvents = Summary.filterTime(mockTimeFilterSummaries, null, nov10End);
    assertThat(mockPastEvents.size()).isEqualTo(4);

    // Test with empty list
    List<Summary> emptyTimeSummaries = new ArrayList<>();
    List<Summary> emptyTimeFiltered = Summary.filterTime(emptyTimeSummaries, nov10Start, nov10End);
    assertThat(emptyTimeFiltered.size()).isEqualTo(0);

    // Test with very specific time range (only midday event)
    Instant specificStart = Instant.parse("2025-11-10T16:00:00Z");
    Instant specificEnd = Instant.parse("2025-11-10T18:00:00Z");
    List<Summary> specificTimeEvents =
        Summary.filterTime(mockTimeFilterSummaries, specificStart, specificEnd);
    assertThat(specificTimeEvents.size()).isEqualTo(1);
    assertThat(specificTimeEvents.get(0).getId()).isEqualTo("midday1");
  }

  @Test
  @AdaptiveTimeout(fast = 1000, slow = 1000)
  @Graded(points = 10, friendlyName = "Test Summary Search Basic (Unit)")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test3_testSummarySearchBasic() {
    // Add your tests here
    // Test with manually created mock data
    List<Summary> mockSearchSummaries = new ArrayList<>();

    // Create mock events with different titles and locations
    Summary basketballGame =
        new Summary(
            "event1", "Thursday Basketball Games", "2025-11-15T18:00:00Z", "Sports Center", false);

    Summary boardGameNight =
        new Summary(
            "event2", "Thursday Board Games Night", "2025-11-15T19:00:00Z", "Student Union", false);

    Summary coffeeShopEvent =
        new Summary(
            "event3", "Study Session", "2025-11-14T10:00:00Z", "Coffee Shop Downtown", false);

    Summary musicConcert =
        new Summary("event4", "JAZZ CONCERT", "2025-11-16T20:00:00Z", "Music Hall", false);

    Summary jazzWorkshop =
        new Summary("event5", "Jazz Workshop", "2025-11-13T14:00:00Z", "Community Center", false);

    Summary gamesAtLibrary =
        new Summary("event6", "Board Games", "2025-11-12T15:00:00Z", "Grainger Library", false);

    // Add all events to the list
    mockSearchSummaries.add(basketballGame);
    mockSearchSummaries.add(boardGameNight);
    mockSearchSummaries.add(coffeeShopEvent);
    mockSearchSummaries.add(musicConcert);
    mockSearchSummaries.add(jazzWorkshop);
    mockSearchSummaries.add(gamesAtLibrary);

    // Test 1: Case-insensitive search by title
    List<Summary> jazzResults = Summary.search(mockSearchSummaries, "jazz");
    assertThat(jazzResults.size()).isEqualTo(2);
    // Results should be sorted alphabetically (case-insensitive)
    assertThat(jazzResults.get(0).getId()).isEqualTo("event4"); // "JAZZ CONCERT"
    assertThat(jazzResults.get(1).getId()).isEqualTo("event5"); // "Jazz Workshop"

    // Test 2: Case-insensitive search with uppercase query
    List<Summary> jazzUppercase = Summary.search(mockSearchSummaries, "JAZZ");
    assertThat(jazzUppercase.size()).isEqualTo(2);
    assertThat(jazzUppercase.get(0).getId()).isEqualTo("event4");

    // Test 3: Case-insensitive search with mixed case
    List<Summary> jazzMixedCase = Summary.search(mockSearchSummaries, "JaZz");
    assertThat(jazzMixedCase.size()).isEqualTo(2);

    // Test 4: Search by location (case-insensitive)
    List<Summary> libraryResults = Summary.search(mockSearchSummaries, "library");
    assertThat(libraryResults.size()).isEqualTo(1);
    assertThat(libraryResults.get(0).getId()).isEqualTo("event6");

    // Test 5: Search by location with different case
    List<Summary> coffeeResults = Summary.search(mockSearchSummaries, "COFFEE");
    assertThat(coffeeResults.size()).isEqualTo(1);
    assertThat(coffeeResults.get(0).getId()).isEqualTo("event3");

    // Test 6: Continuous word search - "Board Games" should match
    List<Summary> boardGamesResults = Summary.search(mockSearchSummaries, "Board Games");
    assertThat(boardGamesResults.size()).isEqualTo(2);
    // Both "Thursday Board Games Night" and "Board Games" should match
    assertThat(boardGamesResults.get(0).getId()).isEqualTo("event6"); // Nov 12
    assertThat(boardGamesResults.get(1).getId()).isEqualTo("event2"); // Nov 15

    // Test 7: Continuous word search - "Thursday Games" should NOT match "Thursday Board Games"
    List<Summary> thursdayGamesResults = Summary.search(mockSearchSummaries, "Thursday Games");
    assertThat(thursdayGamesResults.size()).isEqualTo(0);
    // "Thursday Board Games" has "Board" between "Thursday" and "Games"

    // Test 8: Search with extra spaces should still work
    List<Summary> jazzWithSpaces = Summary.search(mockSearchSummaries, "  jazz  ");
    assertThat(jazzWithSpaces.size()).isEqualTo(2);

    // Test 9: Search for full phrase
    List<Summary> thursdayBoardGames = Summary.search(mockSearchSummaries, "Thursday Board Games");
    assertThat(thursdayBoardGames.size()).isEqualTo(1);
    assertThat(thursdayBoardGames.get(0).getId()).isEqualTo("event2");

    // Test 10: Search that matches both title and location
    List<Summary> boardResults = Summary.search(mockSearchSummaries, "board");
    assertThat(boardResults.size()).isEqualTo(2);
    assertThat(boardResults.get(0).getId())
        .isEqualTo("event6"); // "Board Games" (alphabetically first)
    assertThat(boardResults.get(1).getId()).isEqualTo("event2"); // "Thursday Board Games Night"

    // Test 11: Empty search query returns all events sorted by date
    List<Summary> emptySearchResults = Summary.search(mockSearchSummaries, "");
    assertThat(emptySearchResults.size()).isEqualTo(6);
    // Verify chronological order by checking dates
    assertThat(emptySearchResults.get(0).getStart()).isEqualTo("2025-11-12T15:00:00Z"); // Nov 12
    assertThat(emptySearchResults.get(1).getStart()).isEqualTo("2025-11-13T14:00:00Z"); // Nov 13
    assertThat(emptySearchResults.get(2).getStart()).isEqualTo("2025-11-14T10:00:00Z"); // Nov 14
    assertThat(emptySearchResults.get(3).getStart())
        .isEqualTo("2025-11-15T18:00:00Z"); // Nov 15 18:00
    assertThat(emptySearchResults.get(4).getStart())
        .isEqualTo("2025-11-15T19:00:00Z"); // Nov 15 19:00
    assertThat(emptySearchResults.get(5).getStart()).isEqualTo("2025-11-16T20:00:00Z"); // Nov 16

    // Test 12: Search with no matches
    List<Summary> noMatchResults = Summary.search(mockSearchSummaries, "Swimming");
    assertThat(noMatchResults.size()).isEqualTo(0);

    // Test 13: Search for partial word at beginning
    List<Summary> studyResults = Summary.search(mockSearchSummaries, "Study");
    assertThat(studyResults.size()).isEqualTo(1);
    assertThat(studyResults.get(0).getId()).isEqualTo("event3");

    // Test 14: Multiple spaces between words in query
    List<Summary> multipleSpaces = Summary.search(mockSearchSummaries, "Board    Games");
    assertThat(multipleSpaces.size()).isEqualTo(2);

    // Original provided tests
    List<Summary> allEvents = Summary.search(SUMMARIES, "");
    assertThat(allEvents.size()).isEqualTo(2756);
    assertThat(allEvents).isNotSameInstanceAs(SUMMARIES);

    List<Summary> exhibitResults = Summary.search(SUMMARIES, "exhibit");
    assertThat(exhibitResults.size()).isEqualTo(209);
    assertThat(exhibitResults.getFirst().getId()).isEqualTo("9f6535630fbe18ad");
    assertThat(exhibitResults.getLast().getId()).isEqualTo("a536590b0211d019");
  }

  @Test
  @AdaptiveTimeout(fast = 1000, slow = 1000)
  @Graded(points = 20, friendlyName = "Test Summary Search Filters (Unit)")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test4_testSummarySearchFilters() {
    // Add your tests here
    // Test with manually created mock data
    List<Summary> mockFilterSummaries = new ArrayList<>();

    // Create mock events at different locations with different virtual status
    Summary unionVirtualEvent =
        new Summary(
            "filter1", "Online Lecture Series", "2025-11-20T14:00:00Z", "Union Building", true);

    Summary unionInPersonEvent =
        new Summary("filter2", "Board Game Night", "2025-11-21T19:00:00Z", "Illinois Union", false);

    Summary graingerVirtualEvent =
        new Summary(
            "filter3", "Virtual Study Group", "2025-11-22T10:00:00Z", "Grainger Library", true);

    Summary graingerInPersonEvent =
        new Summary(
            "filter4", "Coffee Chat", "2025-11-23T15:00:00Z", "Grainger Library Main Floor", false);

    Summary siebelInPersonEvent =
        new Summary(
            "filter5", "Programming Workshop", "2025-11-24T16:00:00Z", "Siebel Center", false);

    Summary coffeeShopEvent =
        new Summary(
            "filter6", "Coffee Tasting Event", "2025-11-25T11:00:00Z", "Downtown Cafe", false);

    // Add all events to the list
    mockFilterSummaries.add(unionVirtualEvent);
    mockFilterSummaries.add(unionInPersonEvent);
    mockFilterSummaries.add(graingerVirtualEvent);
    mockFilterSummaries.add(graingerInPersonEvent);
    mockFilterSummaries.add(siebelInPersonEvent);
    mockFilterSummaries.add(coffeeShopEvent);

    // Test 1: location:union - search for events at location union
    List<Summary> mockUnionResults = Summary.search(mockFilterSummaries, "location:union");
    assertThat(mockUnionResults.size()).isEqualTo(2);
    assertThat(mockUnionResults.get(0).getId()).isEqualTo("filter2"); // "Board Game Night"
    assertThat(mockUnionResults.get(1).getId()).isEqualTo("filter1"); // "Online Lecture Series"

    // Test 2: location:grainger - search for locations with grainger
    List<Summary> mockGraingerResults = Summary.search(mockFilterSummaries, "location:grainger");
    assertThat(mockGraingerResults.size()).isEqualTo(2);
    assertThat(mockGraingerResults.get(0).getId()).isEqualTo("filter4"); // "Coffee Chat"
    assertThat(mockGraingerResults.get(1).getId()).isEqualTo("filter3"); // "Virtual Study Group"

    // Test 3: board location:union - search for "board" in title with location union
    List<Summary> mockBoardUnion = Summary.search(mockFilterSummaries, "board location:union");
    assertThat(mockBoardUnion.size()).isEqualTo(1);
    assertThat(mockBoardUnion.get(0).getId()).isEqualTo("filter2");

    // Test 4: location:union board - should NOT work like "board location:union"
    List<Summary> mockUnionBoard = Summary.search(mockFilterSummaries, "location:union board");
    assertThat(mockUnionBoard.size()).isEqualTo(0);

    // Test 5: location:grainger library - spaces between words in location filter
    List<Summary> mockGraingerLibrary =
        Summary.search(mockFilterSummaries, "location:grainger library");
    assertThat(mockGraingerLibrary.size()).isEqualTo(2);

    // Test 6: virtual:true - show virtual events
    List<Summary> mockVirtualTrue = Summary.search(mockFilterSummaries, "virtual:true");
    assertThat(mockVirtualTrue.size()).isEqualTo(2);
    assertThat(mockVirtualTrue.get(0).getId()).isEqualTo("filter1"); // "Online Lecture Series"
    assertThat(mockVirtualTrue.get(1).getId()).isEqualTo("filter3"); // "Virtual Study Group"

    // Test 7: virtual:false - show non-virtual events
    List<Summary> mockVirtualFalse = Summary.search(mockFilterSummaries, "virtual:false");
    assertThat(mockVirtualFalse.size()).isEqualTo(4);

    // Test 8: virtual:blah - invalid virtual value, no matches
    List<Summary> mockVirtualInvalid = Summary.search(mockFilterSummaries, "virtual:blah");
    assertThat(mockVirtualInvalid.size()).isEqualTo(0);

    // Test 9: location:union virtual:false - non-virtual events at union
    List<Summary> mockUnionNotVirtual =
        Summary.search(mockFilterSummaries, "location:union virtual:false");
    assertThat(mockUnionNotVirtual.size()).isEqualTo(1);
    assertThat(mockUnionNotVirtual.get(0).getId()).isEqualTo("filter2");

    // Test 10: location:union virtual:true - virtual events at union
    List<Summary> mockUnionVirtual =
        Summary.search(mockFilterSummaries, "location:union virtual:true");
    assertThat(mockUnionVirtual.size()).isEqualTo(1);
    assertThat(mockUnionVirtual.get(0).getId()).isEqualTo("filter1");

    // Test 11: location:union virtual:blah - no matches due to invalid virtual
    List<Summary> mockUnionVirtualInvalid =
        Summary.search(mockFilterSummaries, "location:union virtual:blah");
    assertThat(mockUnionVirtualInvalid.size()).isEqualTo(0);

    // Test 12: Coffee:drink - looks like filter but not valid, no matches
    List<Summary> mockCoffeeDrink = Summary.search(mockFilterSummaries, "Coffee:drink");
    assertThat(mockCoffeeDrink.size()).isEqualTo(0);

    // Test 13: location:union Coffee:drink - no matches due to invalid filter
    List<Summary> mockUnionCoffeeDrink =
        Summary.search(mockFilterSummaries, "location:union Coffee:drink");
    assertThat(mockUnionCoffeeDrink.size()).isEqualTo(0);

    // Test 14: location:     grainger     library - extra spaces should work
    List<Summary> mockGraingerSpaces =
        Summary.search(mockFilterSummaries, "location:     grainger     library");
    assertThat(mockGraingerSpaces.size()).isEqualTo(2);

    // Test 15: Case insensitive filters - location:UNION
    List<Summary> mockUnionUppercase = Summary.search(mockFilterSummaries, "location:UNION");
    assertThat(mockUnionUppercase.size()).isEqualTo(2);

    // Test 16: Case insensitive filters - VIRTUAL:TRUE
    List<Summary> mockVirtualUppercase = Summary.search(mockFilterSummaries, "VIRTUAL:TRUE");
    assertThat(mockVirtualUppercase.size()).isEqualTo(2);

    // Original provided tests
    List<Summary> unionResults = Summary.search(SUMMARIES, "location:union");
    assertThat(unionResults.size()).isEqualTo(108);

    List<Summary> virtualResults = Summary.search(SUMMARIES, "virtual:true");
    assertThat(virtualResults.size()).isEqualTo(105);

    List<Summary> coffeeAtUnion = Summary.search(SUMMARIES, "coffee location:union");
    assertThat(coffeeAtUnion.size()).isEqualTo(0);
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
