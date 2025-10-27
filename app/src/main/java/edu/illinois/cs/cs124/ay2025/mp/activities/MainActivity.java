package edu.illinois.cs.cs124.ay2025.mp.activities;

import android.app.Activity;
import android.graphics.Insets;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowInsets;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import edu.illinois.cs.cs124.ay2025.mp.R;
import edu.illinois.cs.cs124.ay2025.mp.adapters.SummaryListAdapter;
import edu.illinois.cs.cs124.ay2025.mp.application.EventableApplication;
import edu.illinois.cs.cs124.ay2025.mp.models.Summary;
import java.util.Collections;
import java.util.List;

public final class MainActivity extends Activity {
  private static final String tag = MainActivity.class.getSimpleName();
  private List<Summary> summaries = Collections.emptyList();
  private SummaryListAdapter listAdapter;

  @Override
  protected void onCreate(@Nullable Bundle bundle) {
    super.onCreate(bundle);

    setContentView(R.layout.activity_main);
    setTitle("Discover Event");

    listAdapter = new SummaryListAdapter(summaries, this);

    RecyclerView recyclerView = findViewById(R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    recyclerView.setAdapter(listAdapter);

    setActionBar(findViewById(R.id.toolbar));

    findViewById(R.id.container)
        .setOnApplyWindowInsetsListener(
            (v, windowInsets) -> {
              Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars());
              v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
              return WindowInsets.CONSUMED;
            });
  }

  @Override
  protected void onResume() {
    super.onResume();
    loadSummaries();
  }

  private void loadSummaries() {
    EventableApplication application = (EventableApplication) getApplication();
    application
        .getClient()
        .getSummaries(
            (result) -> {
              try {
                summaries = result.getValue();
                runOnUiThread(this::updateDisplayedSummaries);
              } catch (Exception e) {
                Log.e(tag, "Error updating summary list", e);
              }
            });
  }

  private void updateDisplayedSummaries() {
    if (summaries == null || summaries.isEmpty()) {
      return;
    }
    listAdapter.setSummaries(summaries);
  }
}
