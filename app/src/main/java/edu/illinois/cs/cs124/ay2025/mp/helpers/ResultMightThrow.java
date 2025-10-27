package edu.illinois.cs.cs124.ay2025.mp.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Objects;

public class ResultMightThrow<T> {
  @Nullable private final T value;
  @Nullable private final Exception exception;

  public ResultMightThrow(@NonNull final T setValue) {
    value = Objects.requireNonNull(setValue);
    exception = null;
  }

  public ResultMightThrow(@NonNull final Exception setException) {
    value = null;
    exception = Objects.requireNonNull(setException);
  }

  @NonNull
  public T getValue() throws RuntimeException {
    if (exception != null) {
      throw new RuntimeException(exception);
    } else {
      return Objects.requireNonNull(value);
    }
  }

  @Nullable
  public Exception getException() {
    return exception;
  }
}
