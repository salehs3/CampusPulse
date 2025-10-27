package edu.illinois.cs.cs124.ay2025.mp.test.helpers;

import static com.google.common.truth.Truth.assertWithMessage;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.Permission;
import java.util.Objects;

public class JSONReadCountSecurityManager extends SecurityManager {
  private final Path jsonPath;
  private int jsonReadCount = 0;

  public JSONReadCountSecurityManager() {
    try {
      jsonPath =
          Path.of(
              Objects.requireNonNull(JSONReadCountSecurityManager.class.getResource("/events.json"))
                  .toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void checkPermission(Permission perm) {}

  @Override
  public void checkPermission(Permission perm, Object context) {}

  @Override
  public void checkRead(String file) {
    try {
      if (Path.of(file).equals(jsonPath)) {
        jsonReadCount++;
      }
    } catch (Exception ignored) {
    }
    super.checkRead(file);
  }

  @Override
  public void checkRead(String file, Object context) {
    try {
      if (Path.of(file).equals(jsonPath)) {
        jsonReadCount++;
      }
    } catch (Exception ignored) {
    }
    super.checkRead(file, context);
  }

  private static final int EXPECTED_READ_COUNT = 3;

  public void checkCount(int count) {
    assertWithMessage("events.json should only be accessed during server start")
        .that(jsonReadCount)
        .isAtMost(count);
  }

  public void checkCount() {
    checkCount(EXPECTED_READ_COUNT);
  }
}
