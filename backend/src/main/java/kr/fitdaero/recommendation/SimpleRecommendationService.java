package kr.fitdaero.recommendation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import kr.fitdaero.dataimport.domain.DataImport;
import kr.fitdaero.dataimport.domain.DataImportRepository;
import kr.fitdaero.dataimport.domain.DataImportSourceType;
import kr.fitdaero.dataimport.domain.DataImportStatus;
import kr.fitdaero.program.domain.AdultEligibility;
import kr.fitdaero.program.domain.ProgramCategory;
import kr.fitdaero.program.domain.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SimpleRecommendationService {

  private static final int MAX_RECOMMENDATIONS = 5;
  private static final Set<String> BEGINNER_KEYWORDS = Set.of("입문", "초급", "기초", "초보");
  private static final BigDecimal GOAL_SCORE = BigDecimal.valueOf(40);
  private static final BigDecimal BEGINNER_SCORE = BigDecimal.valueOf(15);
  private static final BigDecimal PREFERRED_CATEGORY_SCORE = BigDecimal.valueOf(15);
  private static final BigDecimal ADULT_EXPLICIT_SCORE = BigDecimal.valueOf(5);
  private static final BigDecimal ADULT_POSSIBLE_SCORE = BigDecimal.valueOf(3);
  private static final BigDecimal WEEKDAY_SCORE = BigDecimal.valueOf(25);
  private static final String GOAL_REASON_TEMPLATE = "%s 목표에 맞는 종목이에요.";
  private static final String BEGINNER_REASON = "초보자도 시작하기 좋은 프로그램이에요.";
  private static final String PREFERRED_CATEGORY_REASON = "선호 종목이에요.";
  private static final String ADULT_EXPLICIT_REASON = "성인 대상이 명확해요.";
  private static final String ADULT_POSSIBLE_REASON = "성인도 참여할 수 있어요.";
  private static final String WEEKDAY_REASON_TEMPLATE = "가능한 요일 중 %s에 참여할 수 있어요.";

  private final DataImportRepository dataImportRepository;
  private final ProgramRepository programRepository;

  public SimpleRecommendationResult recommend(SimpleRecommendationRequest request) {
    DataImport dataImport =
        dataImportRepository
            .findFirstBySourceTypeAndStatusOrderByCompletedAtDescIdDesc(
                DataImportSourceType.PUBLIC_FACILITY_PROGRAM, DataImportStatus.COMPLETED)
            .orElse(null);
    if (dataImport == null) {
      return new SimpleRecommendationResult(null, scopeFor(request), List.of());
    }

    int weekdayMask = Weekday.maskOf(request.weekdays());
    List<RecommendationCandidateProjection> candidates;
    SearchScope scope;
    if (request.sigunguCode() == null || request.sigunguCode().isBlank()) {
      candidates =
          programRepository.findRecommendationCandidatesBySido(
              request.sidoCode(), dataImport.getId(), LocalDate.now(), weekdayMask);
      candidates = withoutAvoidedCategories(candidates, request.avoidedCategories());
      scope = SearchScope.SIDO;
    } else {
      candidates =
          programRepository.findRecommendationCandidatesBySidoAndSigungu(
              request.sidoCode(),
              request.sigunguCode(),
              dataImport.getId(),
              LocalDate.now(),
              weekdayMask);
      candidates = withoutAvoidedCategories(candidates, request.avoidedCategories());
      if (candidates.isEmpty()) {
        candidates =
            programRepository.findRecommendationCandidatesBySido(
                request.sidoCode(), dataImport.getId(), LocalDate.now(), weekdayMask);
        candidates = withoutAvoidedCategories(candidates, request.avoidedCategories());
        scope = SearchScope.SIDO_FALLBACK;
      } else {
        scope = SearchScope.SIGUNGU;
      }
    }

    List<ScoredCandidate> scored =
        candidates.stream().map(candidate -> score(candidate, request, weekdayMask)).toList();
    long normalizedCount =
        scored.stream().filter(candidate -> candidate.category() != ProgramCategory.OTHER).count();

    return new SimpleRecommendationResult(
        dataImport.getDataVersion(),
        scope,
        scored.stream()
            .filter(
                candidate ->
                    normalizedCount < MAX_RECOMMENDATIONS
                        || candidate.category() != ProgramCategory.OTHER)
            .sorted(
                Comparator.comparing(ScoredCandidate::score)
                    .reversed()
                    .thenComparing(ScoredCandidate::adultRank, Comparator.reverseOrder())
                    .thenComparing(ScoredCandidate::commonWeekdays, Comparator.reverseOrder())
                    .thenComparing(ScoredCandidate::endsOn, Comparator.reverseOrder())
                    .thenComparing(ScoredCandidate::programName))
            .limit(MAX_RECOMMENDATIONS)
            .map(ScoredCandidate::toResult)
            .toList());
  }

  private SearchScope scopeFor(SimpleRecommendationRequest request) {
    return request.sigunguCode() == null || request.sigunguCode().isBlank()
        ? SearchScope.SIDO
        : SearchScope.SIDO_FALLBACK;
  }

  private List<RecommendationCandidateProjection> withoutAvoidedCategories(
      List<RecommendationCandidateProjection> candidates, Set<ProgramCategory> avoidedCategories) {
    return candidates.stream()
        .filter(candidate -> !avoidedCategories.contains(category(candidate)))
        .toList();
  }

  private ScoredCandidate score(
      RecommendationCandidateProjection candidate,
      SimpleRecommendationRequest request,
      int userWeekdayMask) {
    ProgramCategory category = category(candidate);
    AdultEligibility adultEligibility = AdultEligibility.valueOf(candidate.getAdultEligibility());
    int commonWeekdays =
        Integer.bitCount(Byte.toUnsignedInt(candidate.getWeekdayMask()) & userWeekdayMask);
    int programWeekdays = Integer.bitCount(Byte.toUnsignedInt(candidate.getWeekdayMask()));
    BigDecimal score = BigDecimal.ZERO;
    List<String> reasons = new ArrayList<>();
    if (request.goal().matches(category)) {
      score = score.add(GOAL_SCORE);
      reasons.add(GOAL_REASON_TEMPLATE.formatted(request.goal().label()));
    }
    if (isBeginnerFriendly(candidate, request)) {
      score = score.add(BEGINNER_SCORE);
      reasons.add(BEGINNER_REASON);
    }
    if (request.preferredCategories().contains(category)) {
      score = score.add(PREFERRED_CATEGORY_SCORE);
      reasons.add(PREFERRED_CATEGORY_REASON);
    }
    if (adultEligibility == AdultEligibility.ADULT_EXPLICIT) {
      score = score.add(ADULT_EXPLICIT_SCORE);
      reasons.add(ADULT_EXPLICIT_REASON);
    } else if (adultEligibility == AdultEligibility.ADULT_POSSIBLE) {
      score = score.add(ADULT_POSSIBLE_SCORE);
      reasons.add(ADULT_POSSIBLE_REASON);
    }
    score =
        score.add(
            WEEKDAY_SCORE
                .multiply(BigDecimal.valueOf(commonWeekdays))
                .divide(BigDecimal.valueOf(programWeekdays), 2, RoundingMode.HALF_UP));
    reasons.add(
        WEEKDAY_REASON_TEMPLATE.formatted(
            Weekday.labelsOf(Byte.toUnsignedInt(candidate.getWeekdayMask()) & userWeekdayMask)));
    return new ScoredCandidate(
        candidate, category, score, reasons, adultEligibility, commonWeekdays);
  }

  private boolean isBeginnerFriendly(
      RecommendationCandidateProjection candidate, SimpleRecommendationRequest request) {
    if (request.activityLevel() != ActivityLevel.NONE
        && request.activityLevel() != ActivityLevel.LOW
        && request.experienceLevel() != ExperienceLevel.BEGINNER) {
      return false;
    }
    String text = Objects.toString(candidate.getTypeName(), "") + " " + candidate.getProgramName();
    return BEGINNER_KEYWORDS.stream().anyMatch(text::contains);
  }

  private ProgramCategory category(RecommendationCandidateProjection candidate) {
    return ProgramCategory.valueOf(candidate.getProgramCategory());
  }

  private record ScoredCandidate(
      RecommendationCandidateProjection candidate,
      ProgramCategory category,
      BigDecimal score,
      List<String> reasons,
      AdultEligibility adultEligibility,
      int commonWeekdays) {

    private int adultRank() {
      return switch (adultEligibility) {
        case ADULT_EXPLICIT -> 2;
        case ADULT_POSSIBLE -> 1;
        default -> 0;
      };
    }

    private LocalDate endsOn() {
      return candidate.getEndsOn();
    }

    private String programName() {
      return candidate.getProgramName();
    }

    private SimpleRecommendationResult.RecommendedProgram toResult() {
      return new SimpleRecommendationResult.RecommendedProgram(
          candidate.getProgramId(),
          candidate.getProgramName(),
          category,
          candidate.getFacilityName(),
          candidate.getAddress(),
          candidate.getStartsOn(),
          candidate.getEndsOn(),
          candidate.getWeekdayText(),
          candidate.getPrice(),
          candidate.getPriceTypeName(),
          candidate.getHomepageUrl(),
          score,
          reasons);
    }
  }
}
