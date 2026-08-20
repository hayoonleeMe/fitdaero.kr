package kr.fitdaero.recommendation;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum Weekday {
  MON(1, "월"),
  TUE(2, "화"),
  WED(4, "수"),
  THU(8, "목"),
  FRI(16, "금"),
  SAT(32, "토"),
  SUN(64, "일");

  private final int mask;
  private final String label;

  Weekday(int mask, String label) {
    this.mask = mask;
    this.label = label;
  }

  public int mask() {
    return mask;
  }

  public String label() {
    return label;
  }

  public static int maskOf(Set<Weekday> weekdays) {
    return weekdays.stream().mapToInt(Weekday::mask).reduce(0, (left, right) -> left | right);
  }

  public static String labelsOf(int weekdayMask) {
    return Arrays.stream(values())
        .filter(weekday -> (weekdayMask & weekday.mask) != 0)
        .map(Weekday::label)
        .collect(Collectors.joining("·"));
  }
}
