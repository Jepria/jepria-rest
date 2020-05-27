package org.jepria.compat.shared;

import org.jepria.compat.shared.util.DefaultComparator;

import java.util.Comparator;

public class AppCompat {

  public static Comparator<Object> getDefaultComparator() {
    return DefaultComparator.instance;
  }
}
