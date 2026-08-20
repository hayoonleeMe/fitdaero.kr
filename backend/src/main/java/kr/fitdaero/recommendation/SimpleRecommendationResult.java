package kr.fitdaero.recommendation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import kr.fitdaero.program.domain.ProgramCategory;

public record SimpleRecommendationResult(
    String publicFacilityProgramDataVersion,
    SearchScope searchScope,
    List<RecommendedProgram> recommendations) {

  public record RecommendedProgram(
      Long programId,
      String programName,
      ProgramCategory category,
      String facilityName,
      String address,
      LocalDate startsOn,
      LocalDate endsOn,
      String weekdayText,
      BigDecimal price,
      String priceTypeName,
      String homepageUrl,
      BigDecimal score,
      List<String> reasons) {}
}
