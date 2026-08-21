package kr.fitdaero.program.importer;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import kr.fitdaero.program.domain.AdultEligibility;
import kr.fitdaero.program.domain.ProgramCategory;

final class ProgramCsvRowNormalizer {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
  private static final Pattern BR_TAG = Pattern.compile("(?i)<br\\s*/?>");
  private static final Pattern WHITESPACE = Pattern.compile("\\s+");
  private static final List<KeywordCategory> KEYWORDS =
      List.of(
          new KeywordCategory(ProgramCategory.SWIMMING_AQUA, "수영", "아쿠아"),
          new KeywordCategory(
              ProgramCategory.FITNESS_STRENGTH, "헬스", "휘트니스", "피트니스", "웨이트", "근력", "보디빌딩"),
          new KeywordCategory(ProgramCategory.YOGA_PILATES, "요가", "필라테스"),
          new KeywordCategory(ProgramCategory.CARDIO, "걷기", "러닝", "달리기", "마라톤", "사이클", "자전거"),
          new KeywordCategory(ProgramCategory.DANCE_AEROBIC, "댄스", "에어로빅", "줌바", "라인댄스"),
          new KeywordCategory(ProgramCategory.RACKET_SPORTS, "테니스", "배드민턴", "탁구", "스쿼시", "라켓볼"),
          new KeywordCategory(ProgramCategory.BALL_SPORTS, "축구", "풋살", "농구", "배구", "야구", "핸드볼"),
          new KeywordCategory(
              ProgramCategory.MARTIAL_ARTS, "태권도", "합기도", "유도", "검도", "복싱", "주짓수", "킥복싱", "무에타이"),
          new KeywordCategory(ProgramCategory.CLIMBING, "클라이밍"),
          new KeywordCategory(ProgramCategory.GOLF, "골프"));

  private ProgramCsvRowNormalizer() {}

  static Optional<ProgramCsvRow> normalize(Map<String, String> row) {
    String facilityName = required(row, "FCLTY_NM");
    String sidoCode = regionCode(row, "CTPRVN_CD", 2);
    String sidoName = required(row, "CTPRVN_NM");
    String sigunguCode = regionCode(row, "SIGNGU_CD", 5);
    String sigunguName = required(row, "SIGNGU_NM");
    String name = required(row, "PROGRM_NM");
    LocalDate startsOn = date(row.get("PROGRM_BEGIN_DE"));
    LocalDate endsOn = date(row.get("PROGRM_END_DE"));
    if (facilityName == null
        || sidoCode == null
        || sidoName == null
        || sigunguCode == null
        || sigunguName == null
        || name == null
        || startsOn == null
        || endsOn == null
        || endsOn.isBefore(startsOn)) {
      return Optional.empty();
    }

    String typeName = text(row.get("PROGRM_TY_NM"));
    String targetName = text(row.get("PROGRM_TRGET_NM"));
    String weekdayText = value(row, "PROGRM_ESTBL_WKDAY_NM");
    return Optional.of(
        new ProgramCsvRow(
            facilityName,
            text(row.get("FCLTY_ADDR")),
            sidoCode,
            sidoName,
            sigunguCode,
            sigunguName,
            nullableText(row.get("EMD_NM")),
            decimal(row.get("FCLTY_LA")),
            decimal(row.get("FCLTY_LO")),
            nullableText(row.get("FCLTY_TEL_NO")),
            nullableText(row.get("HMPG_URL")),
            typeName,
            name,
            targetName,
            startsOn,
            endsOn,
            weekdayText,
            weekdayMask(weekdayText),
            text(row.get("PROGRM_ESTBL_TIZN_VALUE")),
            integer(row.get("PROGRM_RCRIT_NMPR_CO")),
            decimal(row.get("PROGRM_PRC")),
            nullableText(row.get("PROGRM_PRC_TY_NM")),
            category(typeName, name),
            adultEligibility(targetName)));
  }

  private static String required(Map<String, String> row, String column) {
    String value = text(row.get(column));
    return value.isEmpty() ? null : value;
  }

  private static String regionCode(Map<String, String> row, String column, int length) {
    String code = required(row, column);
    return code != null && code.length() == 10 ? code.substring(0, length) : null;
  }

  private static LocalDate date(String value) {
    try {
      return LocalDate.parse(text(value), DATE_FORMATTER);
    } catch (DateTimeException exception) {
      return null;
    }
  }

  private static String value(Map<String, String> row, String column) {
    String value = row.get(column);
    return value == null ? "" : value;
  }

  private static String nullableText(String value) {
    String normalized = text(value);
    return normalized.isEmpty() ? null : normalized;
  }

  private static Integer integer(String value) {
    try {
      return Integer.valueOf(text(value));
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private static BigDecimal decimal(String value) {
    try {
      return new BigDecimal(text(value));
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private static String text(String value) {
    return WHITESPACE
        .matcher(BR_TAG.matcher(value == null ? "" : value).replaceAll(" "))
        .replaceAll(" ")
        .trim();
  }

  private static Byte weekdayMask(String weekdayText) {
    if (weekdayText.contains("매일")) {
      return 127;
    }

    byte mask = 0;
    String[] weekdays = {"월", "화", "수", "목", "금", "토", "일"};
    for (int index = 0; index < weekdays.length; index++) {
      if (weekdayText.contains(weekdays[index])) {
        mask |= (byte) (1 << index);
      }
    }
    return mask == 0 ? null : mask;
  }

  private static ProgramCategory category(String typeName, String name) {
    return findCategory(typeName).or(() -> findCategory(name)).orElse(ProgramCategory.OTHER);
  }

  private static Optional<ProgramCategory> findCategory(String text) {
    int earliestIndex = Integer.MAX_VALUE;
    ProgramCategory category = null;
    for (KeywordCategory keywordCategory : KEYWORDS) {
      for (String keyword : keywordCategory.keywords()) {
        int index = text.indexOf(keyword);
        if (index >= 0 && index < earliestIndex) {
          earliestIndex = index;
          category = keywordCategory.category();
        }
      }
    }
    return Optional.ofNullable(category);
  }

  private static AdultEligibility adultEligibility(String targetName) {
    if (containsAny(targetName, "성인", "실버", "노인", "경로")
        || targetName.matches(".*65\\s*세\\s*이상.*")) {
      return AdultEligibility.ADULT_EXPLICIT;
    }
    if (containsAny(targetName, "전체", "누구나", "일반")
        || targetName.matches(".*(?:유아|어린이|청소년|중학생|중고생|\\d+\\s*세)\\s*이상.*")) {
      return AdultEligibility.ADULT_POSSIBLE;
    }
    if (containsAny(targetName, "유아", "어린이", "초등", "미취학", "청소년")) {
      return AdultEligibility.CHILD_ONLY;
    }
    return AdultEligibility.UNKNOWN;
  }

  private static boolean containsAny(String text, String... values) {
    for (String value : values) {
      if (text.contains(value)) {
        return true;
      }
    }
    return false;
  }

  private record KeywordCategory(ProgramCategory category, String... keywords) {}
}
