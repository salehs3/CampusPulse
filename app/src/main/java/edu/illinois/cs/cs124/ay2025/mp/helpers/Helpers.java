package edu.illinois.cs.cs124.ay2025.mp.helpers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.time.Instant;

public final class Helpers {
  public static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));

  public static final String CHECK_SERVER_RESPONSE = "AY2025";

  public interface TimeProvider {
    Instant now();
  }

  public static class SystemTimeProvider implements TimeProvider {
    @Override
    public Instant now() {
      return Instant.now();
    }
  }

  private static TimeProvider timeProvider = new SystemTimeProvider();

  public static TimeProvider getTimeProvider() {
    return timeProvider;
  }

  public static void setTimeProvider(TimeProvider provider) {
    timeProvider = provider;
  }
}
