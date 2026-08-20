package kr.fitdaero.recommendation;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RecommendationCandidateProjection {

  Long getProgramId();

  String getProgramName();

  String getTypeName();

  String getProgramCategory();

  String getFacilityName();

  String getAddress();

  LocalDate getStartsOn();

  LocalDate getEndsOn();

  String getWeekdayText();

  Byte getWeekdayMask();

  BigDecimal getPrice();

  String getPriceTypeName();

  String getHomepageUrl();

  String getAdultEligibility();
}
