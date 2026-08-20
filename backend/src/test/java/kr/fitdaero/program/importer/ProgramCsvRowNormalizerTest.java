package kr.fitdaero.program.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import kr.fitdaero.program.domain.AdultEligibility;
import kr.fitdaero.program.domain.ProgramCategory;
import org.junit.jupiter.api.Test;

class ProgramCsvRowNormalizerTest {

  @Test
  void normalizesRowForRecommendation() {
    ProgramCsvRow row =
        ProgramCsvRowNormalizer.normalize(
                row(
                    "PROGRM_NM", "  아쿠아<br/> 요가  ",
                    "PROGRM_TY_NM", "  헬스  ",
                    "PROGRM_TRGET_NM", " 성인 / 청소년 ",
                    "PROGRM_ESTBL_WKDAY_NM", "월, 수, 금",
                    "PROGRM_ESTBL_TIZN_VALUE", " 10:00 <br> 11:00 "))
            .orElseThrow();

    assertThat(row.name()).isEqualTo("아쿠아 요가");
    assertThat(row.typeName()).isEqualTo("헬스");
    assertThat(row.targetName()).isEqualTo("성인 / 청소년");
    assertThat(row.timeText()).isEqualTo("10:00 11:00");
    assertThat(row.startsOn()).isEqualTo(LocalDate.of(2026, 8, 1));
    assertThat(row.endsOn()).isEqualTo(LocalDate.of(2026, 8, 31));
    assertThat(row.weekdayMask()).isEqualTo((byte) 21);
    assertThat(row.programCategory()).isEqualTo(ProgramCategory.FITNESS_STRENGTH);
    assertThat(row.adultEligibility()).isEqualTo(AdultEligibility.ADULT_EXPLICIT);
  }

  @Test
  void handlesWeekdayAndTargetSpecialCases() {
    assertThat(
            ProgramCsvRowNormalizer.normalize(row("PROGRM_ESTBL_WKDAY_NM", "매일"))
                .orElseThrow()
                .weekdayMask())
        .isEqualTo((byte) 127);
    assertThat(
            ProgramCsvRowNormalizer.normalize(row("PROGRM_ESTBL_WKDAY_NM", "상시"))
                .orElseThrow()
                .weekdayMask())
        .isNull();
    assertThat(
            ProgramCsvRowNormalizer.normalize(row("PROGRM_TRGET_NM", "청소년이상"))
                .orElseThrow()
                .adultEligibility())
        .isEqualTo(AdultEligibility.ADULT_POSSIBLE);
    assertThat(
            ProgramCsvRowNormalizer.normalize(row("PROGRM_TRGET_NM", "초등학생"))
                .orElseThrow()
                .adultEligibility())
        .isEqualTo(AdultEligibility.CHILD_ONLY);
  }

  @Test
  void usesLeftmostKeywordWhenTypeDoesNotMatch() {
    assertThat(
            ProgramCsvRowNormalizer.normalize(row("PROGRM_TY_NM", "문화", "PROGRM_NM", "요가와 수영"))
                .orElseThrow()
                .programCategory())
        .isEqualTo(ProgramCategory.YOGA_PILATES);
  }

  @Test
  void excludesRowsWithMissingOrInvalidRequiredValues() {
    assertThat(ProgramCsvRowNormalizer.normalize(row("FCLTY_NM", " "))).isEmpty();
    assertThat(ProgramCsvRowNormalizer.normalize(row("PROGRM_BEGIN_DE", "20260230"))).isEmpty();
    assertThat(
            ProgramCsvRowNormalizer.normalize(
                row("PROGRM_BEGIN_DE", "20260831", "PROGRM_END_DE", "20260801")))
        .isEmpty();
  }

  private Map<String, String> row(String... values) {
    Map<String, String> row = new HashMap<>();
    row.put("FCLTY_NM", "핏대로 체육관");
    row.put("FCLTY_ADDR", "서울특별시 종로구 운동로 1");
    row.put("CTPRVN_CD", "11");
    row.put("CTPRVN_NM", "서울특별시");
    row.put("SIGNGU_CD", "11000");
    row.put("SIGNGU_NM", "종로구");
    row.put("PROGRM_TY_NM", "수영");
    row.put("PROGRM_NM", "성인 수영");
    row.put("PROGRM_TRGET_NM", "성인");
    row.put("PROGRM_BEGIN_DE", "20260801");
    row.put("PROGRM_END_DE", "20260831");
    row.put("PROGRM_ESTBL_WKDAY_NM", "월");
    row.put("PROGRM_ESTBL_TIZN_VALUE", "10:00");
    for (int index = 0; index < values.length; index += 2) {
      row.put(values[index], values[index + 1]);
    }
    return row;
  }
}
