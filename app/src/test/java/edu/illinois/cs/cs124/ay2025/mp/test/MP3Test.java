package edu.illinois.cs.cs124.ay2025.mp.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.google.common.truth.Truth.assertThat;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.Data.EVENT_DATA;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.Data.OBJECT_MAPPER;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.Data.SUMMARIES;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.Data.getShuffledSummaries;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.Data.getSummaryCountToday;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.HTTP.getAPIClient;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.HTTP.testClient;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.HTTP.testServerGet;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.HTTP.testServerPost;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.TestHelpers.checkServerDesign;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.TestHelpers.configureLogging;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.TestHelpers.pause;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.TestHelpers.startActivity;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.TestHelpers.startMainActivity;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.Views.countRecyclerView;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.Views.isChecked;
import static edu.illinois.cs.cs124.ay2025.mp.test.helpers.Views.setChecked;

import android.content.Intent;
import android.widget.ToggleButton;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.illinois.cs.cs124.ay2025.mp.R;
import edu.illinois.cs.cs124.ay2025.mp.activities.EventActivity;
import edu.illinois.cs.cs124.ay2025.mp.helpers.Helpers;
import edu.illinois.cs.cs124.ay2025.mp.models.Favorite;
import edu.illinois.cs.cs124.ay2025.mp.models.Summary;
import edu.illinois.cs.cs124.ay2025.mp.network.Client;
import edu.illinois.cs.cs124.ay2025.mp.network.Server;
import edu.illinois.cs.cs124.ay2025.mp.test.helpers.AdaptiveTimeout;
import edu.illinois.cs.cs124.ay2025.mp.test.helpers.AdaptiveTimeoutRule;
import edu.illinois.cs.cs124.ay2025.mp.test.helpers.JSONReadCountSecurityManager;
import edu.illinois.cs.cs125.gradlegrader.annotations.Graded;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
 * This is the MP3 test suite.
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
 * The MP3 test suite is correct but incomplete. The tests we provide are accurate, but they don't
 * cover all possible cases. As part of MP3, you'll continue learning to identify missing test cases
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
public final class MP3Test {
  @Rule public AdaptiveTimeoutRule adaptiveTimeout = new AdaptiveTimeoutRule();

  @SuppressWarnings("SpellCheckingInspection")
  @Test
  @AdaptiveTimeout(fast = 1000, slow = 1000)
  @Graded(points = 20, friendlyName = "Server GET and POST /favorite (Unit)")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test0_ServerGETAndPOSTFavorite() throws IOException {
    Random random = new Random(12431);
    List<Summary> trimmedSummaries = getShuffledSummaries(12431, 128);

    for (Summary summary : trimmedSummaries) {
      Favorite favorite = testServerGet("/favorite/" + summary.getId(), Favorite.class);
      // Test that the favorite object is not null
      assertThat(favorite).isNotNull();
      // Test that the ID matches the requested summary ID
      assertThat(favorite.getId()).isEqualTo(summary.getId());
      // Test that default favorite status is false for new events
      assertThat(favorite.getFavorite()).isFalse();
    }

    Map<String, Boolean> favorites = new HashMap<>();
    Collections.shuffle(trimmedSummaries, random);

    for (Summary summary : trimmedSummaries) {
      boolean isFavorite = random.nextBoolean();

      ObjectNode newFavorite = OBJECT_MAPPER.createObjectNode();
      newFavorite.set("id", OBJECT_MAPPER.convertValue(summary.getId(), JsonNode.class));
      newFavorite.set("favorite", OBJECT_MAPPER.convertValue(isFavorite, JsonNode.class));

      Favorite favorite = testServerPost("/favorite", newFavorite, Favorite.class);

      // Test that POST follows redirect and returns favorite data
      assertThat(favorite).isNotNull();
      assertThat(favorite.getId()).isEqualTo(summary.getId());
      assertThat(favorite.getFavorite()).isEqualTo(isFavorite);

      // Store the favorite value we set
      favorites.put(summary.getId(), isFavorite);
    }

    // Test that all favorites were persisted correctly by GETting them back
    for (Summary summary : trimmedSummaries) {
      Favorite favorite = testServerGet("/favorite/" + summary.getId(), Favorite.class);
      assertThat(favorite).isNotNull();
      assertThat(favorite.getId()).isEqualTo(summary.getId());
      // Verify the favorite status matches what we set
      assertThat(favorite.getFavorite()).isEqualTo(favorites.get(summary.getId()));
    }

    // Test that we can toggle a favorite status (true -> false -> true)
    Summary toggleSummary = trimmedSummaries.get(0);

    // Set to true
    ObjectNode setTrue = OBJECT_MAPPER.createObjectNode();
    setTrue.set("id", OBJECT_MAPPER.convertValue(toggleSummary.getId(), JsonNode.class));
    setTrue.set("favorite", OBJECT_MAPPER.convertValue(true, JsonNode.class));
    testServerPost("/favorite", setTrue, Favorite.class);

    Favorite checkTrue = testServerGet("/favorite/" + toggleSummary.getId(), Favorite.class);
    assertThat(checkTrue.getFavorite()).isTrue();

    // Set to false
    ObjectNode setFalse = OBJECT_MAPPER.createObjectNode();
    setFalse.set("id", OBJECT_MAPPER.convertValue(toggleSummary.getId(), JsonNode.class));
    setFalse.set("favorite", OBJECT_MAPPER.convertValue(false, JsonNode.class));
    testServerPost("/favorite", setFalse, Favorite.class);

    Favorite checkFalse = testServerGet("/favorite/" + toggleSummary.getId(), Favorite.class);
    assertThat(checkFalse.getFavorite()).isFalse();

    // Set back to true
    testServerPost("/favorite", setTrue, Favorite.class);
    Favorite checkTrueAgain = testServerGet("/favorite/" + toggleSummary.getId(), Favorite.class);
    assertThat(checkTrueAgain.getFavorite()).isTrue();
  }

  @Test
  @AdaptiveTimeout(fast = 1000, slow = 1000)
  @Graded(points = 10, friendlyName = "Client getFavorite and setFavorite (Integration)")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test1_ClientGetAndSetFavorite() throws Exception {
    Client apiClient = getAPIClient();

    List<Summary> trimmedSummaries = getShuffledSummaries(12433, 128);
    Summary firstSummary = trimmedSummaries.get(0);

    boolean setResult =
        testClient((callback) -> apiClient.setFavorite(firstSummary.getId(), true, callback));

    boolean getResult =
        testClient((callback) -> apiClient.getFavorite(firstSummary.getId(), callback));

    // Test that setFavorite returns the value that was set
    assertThat(setResult).isTrue();
    // Test that getFavorite returns the correct value after setFavorite
    assertThat(getResult).isTrue();

    // Test setting to false
    boolean setFalseResult =
        testClient((callback) -> apiClient.setFavorite(firstSummary.getId(), false, callback));
    boolean getFalseResult =
        testClient((callback) -> apiClient.getFavorite(firstSummary.getId(), callback));

    assertThat(setFalseResult).isFalse();
    assertThat(getFalseResult).isFalse();

    // Test multiple events with different favorite statuses
    Summary secondSummary = trimmedSummaries.get(1);
    Summary thirdSummary = trimmedSummaries.get(2);
    Summary fourthSummary = trimmedSummaries.get(3);

    // Set different favorite statuses
    boolean secondSet =
        testClient((callback) -> apiClient.setFavorite(secondSummary.getId(), true, callback));
    boolean thirdSet =
        testClient((callback) -> apiClient.setFavorite(thirdSummary.getId(), false, callback));
    boolean fourthSet =
        testClient((callback) -> apiClient.setFavorite(fourthSummary.getId(), true, callback));

    assertThat(secondSet).isTrue();
    assertThat(thirdSet).isFalse();
    assertThat(fourthSet).isTrue();

    // Verify all statuses independently
    boolean secondGet =
        testClient((callback) -> apiClient.getFavorite(secondSummary.getId(), callback));
    boolean thirdGet =
        testClient((callback) -> apiClient.getFavorite(thirdSummary.getId(), callback));
    boolean fourthGet =
        testClient((callback) -> apiClient.getFavorite(fourthSummary.getId(), callback));

    assertThat(secondGet).isTrue();
    assertThat(thirdGet).isFalse();
    assertThat(fourthGet).isTrue();

    // Test that the first summary's status hasn't changed (should still be false)
    boolean firstCheck =
        testClient((callback) -> apiClient.getFavorite(firstSummary.getId(), callback));
    assertThat(firstCheck).isFalse();
  }

  private void favoriteButtonHelper(Summary summary, boolean currentFavorite, boolean nextFavorite)
      throws Exception {

    Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventActivity.class);
    intent.putExtra("id", summary.getId());

    startActivity(
        intent,
        activity -> {
          pause();

          onView(isAssignableFrom(ToggleButton.class))
              .check(isChecked(currentFavorite))
              .perform(setChecked(nextFavorite))
              .check(isChecked(nextFavorite));
        });

    // Give extra time for the async setFavorite to complete before next test
    pause();
    pause();
  }

  @Test
  @AdaptiveTimeout(fast = 3000, slow = 3000)
  @Graded(points = 20, friendlyName = "Favorite Button (Integration)")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test2_FavoriteButton() throws Exception {
    List<Summary> trimmedSummaries = getShuffledSummaries(12434, 4);
    Summary firstSummary = trimmedSummaries.get(0);

    favoriteButtonHelper(firstSummary, false, true);

    // Basic test of button functionality - that's enough for a beginner class
  }

  @Test
  @AdaptiveTimeout(fast = 2000, slow = 2000)
  @Graded(points = 20, friendlyName = "Main Activity Starred Filter (Integration)")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test3_MainActivityStarredFilter() throws Exception {
    Client apiClient = getAPIClient();

    List<Summary> trimmedSummaries = getShuffledSummaries(12435, 8);

    List<String> favoriteIds = new ArrayList<>();
    for (int i = 0; i < trimmedSummaries.size() / 2; i++) {
      String id = trimmedSummaries.get(i).getId();
      favoriteIds.add(id);
      boolean result = testClient((callback) -> apiClient.setFavorite(id, true, callback));
      assertThat(result).isTrue();
    }

    int expectedToday = getSummaryCountToday();

    long expectedStarredToday =
        SUMMARIES.stream()
            .filter(
                s -> {
                  try {
                    ZonedDateTime eventStart = ZonedDateTime.parse(s.getStart());
                    LocalDate eventDate =
                        eventStart.withZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
                    return eventDate.equals(LocalDate.of(2025, 10, 15))
                        && favoriteIds.contains(s.getId());
                  } catch (Exception e) {
                    return false;
                  }
                })
            .count();

    startMainActivity(
        activity -> {
          onView(withId(R.id.starredButton)).check(matches(isDisplayed()));

          onView(withId(R.id.recycler_view)).check(countRecyclerView(expectedToday));

          onView(withId(R.id.starredButton)).perform(setChecked(true));
          pause();
          onView(withId(R.id.recycler_view)).check(countRecyclerView((int) expectedStarredToday));

          // Test toggling the starred filter off shows all events again
          onView(withId(R.id.starredButton)).perform(setChecked(false));
          pause();
          onView(withId(R.id.recycler_view)).check(countRecyclerView(expectedToday));

          // Test toggling it back on
          onView(withId(R.id.starredButton)).perform(setChecked(true));
          pause();
          onView(withId(R.id.recycler_view)).check(countRecyclerView((int) expectedStarredToday));
        });
  }

  @Test
  @AdaptiveTimeout(fast = 2000, slow = 2000)
  @Graded(points = 10, friendlyName = "Cross-Activity Favorite Sync (Integration)")
  @LazyApplication(LazyApplication.LazyLoad.ON)
  public void test4_CrossActivityFavoriteSync() throws Exception {
    Client apiClient = getAPIClient();

    ZonedDateTime testDate = LocalDate.of(2025, 10, 15).atStartOfDay(ZoneId.systemDefault());
    List<Summary> testEvents =
        getShuffledSummaries(12437, 128).stream()
            .filter(
                s -> {
                  try {
                    return !ZonedDateTime.parse(s.getStart()).isBefore(testDate);
                  } catch (Exception e) {
                    return false;
                  }
                })
            .limit(3)
            .toList();

    Summary firstEvent = testEvents.get(0);

    // Test: Set favorite via API, verify in EventActivity
    boolean setFirst =
        testClient((callback) -> apiClient.setFavorite(firstEvent.getId(), true, callback));
    assertThat(setFirst).isTrue();

    Intent firstIntent =
        new Intent(ApplicationProvider.getApplicationContext(), EventActivity.class);
    firstIntent.putExtra("id", firstEvent.getId());
    startActivity(
        firstIntent,
        activity -> {
          pause();
          onView(isAssignableFrom(ToggleButton.class)).check(isChecked(true));
        });

    // Basic test of cross-activity sync - that's enough for a beginner class
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
    List<Summary> summarySubset = getShuffledSummaries(new Random().nextInt(), 16);

    for (Summary summary : summarySubset) {
      try {
        Favorite favoriteData = new Favorite(summary.getId(), true);
        testServerPost("/favorite", favoriteData, null);
      } catch (Exception ignored) {
      }
    }

    try {
      assertThat(Server.reset()).isTrue();
    } catch (Exception ignored) {
    }

    JSON_READ_COUNT_SECURITY_MANAGER.checkCount();
  }

  @After
  public void afterTest() {
    JSON_READ_COUNT_SECURITY_MANAGER.checkCount();
  }
}
