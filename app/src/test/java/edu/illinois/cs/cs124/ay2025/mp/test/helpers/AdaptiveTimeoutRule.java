package edu.illinois.cs.cs124.ay2025.mp.test.helpers;

import java.io.InterruptedIOException;
import java.util.concurrent.TimeUnit;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestTimedOutException;
import org.robolectric.junit.rules.TimeoutRule;

public class AdaptiveTimeoutRule implements TestRule {
  private static boolean anyTestClassSeen = false;

  @Override
  public Statement apply(Statement base, Description description) {
    AdaptiveTimeout annotation = description.getAnnotation(AdaptiveTimeout.class);
    if (annotation == null) {
      return base;
    }

    boolean isInitialized = anyTestClassSeen;
    anyTestClassSeen = true;

    final long baseTimeoutMs = isInitialized ? annotation.fast() : annotation.slow();

    String multiplierStr = System.getenv("TEST_TIMEOUT_MULTIPLIER");
    if (multiplierStr == null) {
      throw new IllegalStateException(
          "TEST_TIMEOUT_MULTIPLIER environment variable must be set. "
              + "Recommended value: 4 for local development, higher for CI/containers.");
    }
    final double multiplier = Double.parseDouble(multiplierStr);
    final long rawTimeout = (long) (baseTimeoutMs * multiplier);

    String minStr = System.getenv("TEST_TIMEOUT_MIN");
    if (minStr == null) {
      throw new IllegalStateException(
          "TEST_TIMEOUT_MIN environment variable must be set. "
              + "Recommended value: 5000 (5 seconds minimum for short tests).");
    }
    final long minTimeout = Long.parseLong(minStr);

    String maxStr = System.getenv("TEST_TIMEOUT_MAX");
    if (maxStr == null) {
      throw new IllegalStateException(
          "TEST_TIMEOUT_MAX environment variable must be set. "
              + "Recommended value: 30000 (30 seconds maximum to prevent excessive waits).");
    }
    final long maxTimeout = Long.parseLong(maxStr);

    final long timeoutMs = Math.max(minTimeout, Math.min(maxTimeout, rawTimeout));

    TimeoutRule robolectricTimeout = TimeoutRule.millis(timeoutMs);
    Statement timeoutStatement = robolectricTimeout.apply(base, description);

    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          timeoutStatement.evaluate();
        } catch (TestTimedOutException e) {
          throw createTimeoutException(e.getMessage());
        } catch (InterruptedIOException e) {
          throw createTimeoutException("Test interrupted during I/O operation (likely timeout)");
        } catch (InterruptedException e) {
          throw createTimeoutException("Test interrupted (likely timeout)");
        } catch (IllegalStateException e) {
          if (e.getCause() instanceof InterruptedException) {
            throw createTimeoutException("Test interrupted (likely timeout)");
          } else {
            throw e;
          }
        }
      }

      private Exception createTimeoutException(String baseMessage) {
        String constraintNote = "";
        if (rawTimeout < minTimeout) {
          constraintNote = String.format(" (raised from %dms to MIN)", rawTimeout);
        } else if (rawTimeout > maxTimeout) {
          constraintNote = String.format(" (capped from %dms to MAX)", rawTimeout);
        }

        String contextInfo =
            String.format(
                """
                    Timeout details:
                      Base timeout: %dms (%s path)
                      Multiplier: %.1f (TEST_TIMEOUT_MULTIPLIER)
                      Raw timeout: %dms (base × multiplier)
                      Constraints: MIN=%dms, MAX=%dms
                      Applied timeout: %dms%s
                      Test: %s""",
                baseTimeoutMs,
                isInitialized ? "fast/suite" : "slow/isolated",
                multiplier,
                rawTimeout,
                minTimeout,
                maxTimeout,
                timeoutMs,
                constraintNote,
                description.getMethodName());
        return new TestTimedOutException(timeoutMs, TimeUnit.MILLISECONDS) {
          @Override
          public String getMessage() {
            return baseMessage + contextInfo;
          }
        };
      }
    };
  }
}
