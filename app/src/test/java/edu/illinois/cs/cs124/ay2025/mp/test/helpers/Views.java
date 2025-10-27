package edu.illinois.cs.cs124.ay2025.mp.test.helpers;

import static androidx.test.espresso.action.ViewActions.actionWithAssertions;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.common.truth.Truth.assertWithMessage;

import android.view.View;
import android.widget.SearchView;
import android.widget.ToggleButton;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.matcher.ViewMatchers;
import java.util.Objects;
import org.hamcrest.Matcher;

public class Views {
  public static ViewAssertion countRecyclerView(int expected) {
    return (v, noViewFoundException) -> {
      if (noViewFoundException != null) {
        throw noViewFoundException;
      }
      RecyclerView view = (RecyclerView) v;
      RecyclerView.Adapter<?> adapter = view.getAdapter();
      assertWithMessage("View adapter should not be null").that(adapter).isNotNull();
      assertWithMessage("Adapter should have " + expected + " items")
          .that(Objects.requireNonNull(adapter).getItemCount())
          .isEqualTo(expected);
    };
  }

  public static ViewAction searchFor(String query) {
    return searchFor(query, false);
  }

  public static ViewAction searchFor(String query, boolean submit) {
    return new ViewAction() {
      @Override
      public Matcher<View> getConstraints() {
        return isDisplayed();
      }

      @Override
      public String getDescription() {
        if (submit) {
          return "Set query to " + query + " and submit";
        } else {
          return "Set query to " + query + " but don't submit";
        }
      }

      @Override
      public void perform(UiController uiController, View view) {
        SearchView searchView = (SearchView) view;
        searchView.setQuery(query, submit);
      }
    };
  }

  public static ViewAssertion isChecked(boolean checked) {
    return (view, noViewFoundException) -> {
      assertWithMessage("Should have found view").that(noViewFoundException).isNull();
      assertWithMessage("View should be a ToggleButton")
          .that(view)
          .isInstanceOf(ToggleButton.class);

      ToggleButton toggleButton = (ToggleButton) view;
      String message;
      if (checked) {
        message = "ToggleButton should be checked";
      } else {
        message = "ToggleButton should not be checked";
      }
      assertWithMessage(message).that(toggleButton.isChecked()).isEqualTo(checked);
    };
  }

  public static ViewAction setChecked(boolean checked) {
    return actionWithAssertions(
        new ViewAction() {
          @Override
          public Matcher<View> getConstraints() {
            return ViewMatchers.isAssignableFrom(ToggleButton.class);
          }

          @Override
          public String getDescription() {
            return "Custom view action to check or uncheck ToggleButton";
          }

          @Override
          public void perform(UiController uiController, View view) {
            ToggleButton toggleButton = (ToggleButton) view;
            toggleButton.setChecked(checked);
          }
        });
  }
}
