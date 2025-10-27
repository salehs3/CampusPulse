package edu.illinois.cs.cs124.ay2025.mp.test.helpers;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

public class FilteringPrintStream extends PrintStream {
  public FilteringPrintStream() {
    super(OutputStream.nullOutputStream());
  }

  private static final List<String> IGNORED_TAGS =
      Arrays.asList(
          "LifecycleMonitor",
          "ActivityScenario",
          "AppCompatDelegate",
          "ViewInteraction",
          "Tracing",
          "EventInjectionStrategy",
          "VirtualDeviceManager",
          "AutofillManager",
          "WindowOnBackDispatcher",
          "FileTestStorage",
          "OverlayConfig",
          "Configuration",
          "ViewRootImpl",
          "FeatureFlagsImplExport",
          "DesktopModeFlags",
          "DisplayManager");

  @Override
  public void println(String line) {
    String[] parts = line.split(": ");
    if (parts.length < 2) {
      System.out.println(line);
      return;
    }
    String[] tagParts = parts[0].split("/");
    if (tagParts.length != 2) {
      System.out.println(line);
      return;
    }
    if (IGNORED_TAGS.contains(tagParts[1])) {
      return;
    }
    System.out.println(line);
  }
}
