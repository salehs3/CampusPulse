package edu.illinois.cs.cs124.ay2025.mp.test.helpers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AdaptiveTimeout {
  long fast();

  long slow();
}
