package edu.illinois.cs.cs124.ay2025.mp.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import edu.illinois.cs.cs124.ay2025.mp.R;
import edu.illinois.cs.cs124.ay2025.mp.models.Summary;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class SummaryListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  @NonNull private List<Summary> summaries;
  @Nullable private final Consumer<Summary> onClickCallback;
  @NonNull private final Activity activity;
  private static final int MAX_LOCATION_LENGTH = 30;
  private static final int TRUNCATED_LOCATION_LENGTH = 27;

  public SummaryListAdapter(
      @NonNull List<Summary> setSummaries,
      @NonNull Activity setActivity,
      @Nullable Consumer<Summary> setOnClickCallback) {
    summaries = setSummaries;
    activity = setActivity;
    onClickCallback = setOnClickCallback;
  }

  public SummaryListAdapter(@NonNull List<Summary> setSummaries, @NonNull Activity setActivity) {
    this(setSummaries, setActivity, null);
  }

  @SuppressLint("NotifyDataSetChanged")
  public void setSummaries(@NonNull List<Summary> setSummaries) {
    summaries = setSummaries;
    this.notifyDataSetChanged();
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_summary, parent, false);
    return new RecyclerView.ViewHolder(view) {};
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    Summary summary = summaries.get(position);

    TextView title = holder.itemView.findViewById(R.id.title);
    TextView dateTimeLocation = holder.itemView.findViewById(R.id.dateTimeLocation);
    LinearLayout layout = holder.itemView.findViewById(R.id.layout);

    title.setText(summary.getTitle());
    title.setTextColor(activity.getColor(R.color.darkTextColor));

    String dateTimeLocationText = formatDateTimeLocation(summary);
    dateTimeLocation.setText(dateTimeLocationText);

    layout.setBackgroundColor(activity.getColor(android.R.color.white));

    if (onClickCallback != null) {
      layout.setOnClickListener(view -> onClickCallback.accept(summary));
    }
  }

  private String formatDateTimeLocation(Summary summary) {
    try {
      ZonedDateTime start = ZonedDateTime.parse(summary.getStart());
      String date = start.format(DateTimeFormatter.ofPattern("MMM d"));
      String time = start.format(DateTimeFormatter.ofPattern("h:mm a"));
      String location = summary.getLocation();
      if (location.isEmpty()) {
        return date + " • " + time;
      }
      if (location.length() > MAX_LOCATION_LENGTH) {
        location = location.substring(0, TRUNCATED_LOCATION_LENGTH) + "...";
      }
      return date + " • " + time + " • " + location;
    } catch (Exception e) {
      String location = summary.getLocation();
      if (location.isEmpty()) {
        return "";
      } else {
        return location;
      }
    }
  }

  @Override
  public int getItemCount() {
    return summaries.size();
  }
}
