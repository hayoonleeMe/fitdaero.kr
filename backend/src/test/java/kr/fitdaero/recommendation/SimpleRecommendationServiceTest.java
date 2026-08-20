package kr.fitdaero.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import kr.fitdaero.dataimport.domain.DataImport;
import kr.fitdaero.dataimport.domain.DataImportRepository;
import kr.fitdaero.dataimport.domain.DataImportSourceType;
import kr.fitdaero.dataimport.domain.DataImportStatus;
import kr.fitdaero.program.domain.AdultEligibility;
import kr.fitdaero.program.domain.ProgramCategory;
import kr.fitdaero.program.domain.ProgramRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SimpleRecommendationServiceTest {

  @Mock private DataImportRepository dataImportRepository;

  @Mock private ProgramRepository programRepository;

  @InjectMocks private SimpleRecommendationService simpleRecommendationService;

  @Test
  void fallsBackToSidoAndReturnsFiveScoredRecommendations() {
    DataImport dataImport = completedImport();
    when(dataImportRepository.findFirstBySourceTypeAndStatusOrderByCompletedAtDescIdDesc(
            DataImportSourceType.PUBLIC_FACILITY_PROGRAM, DataImportStatus.COMPLETED))
        .thenReturn(Optional.of(dataImport));
    when(programRepository.findRecommendationCandidatesBySidoAndSigungu(
            eq("11"), eq("11000"), eq(1L), any(LocalDate.class), eq(21)))
        .thenReturn(List.of());
    when(programRepository.findRecommendationCandidatesBySido(
            eq("11"), eq(1L), any(LocalDate.class), eq(21)))
        .thenReturn(
            List.of(
                candidate(
                    "초급 수영",
                    "수영",
                    ProgramCategory.SWIMMING_AQUA,
                    AdultEligibility.ADULT_EXPLICIT,
                    5),
                candidate("러닝", "러닝", ProgramCategory.CARDIO, AdultEligibility.ADULT_POSSIBLE, 17),
                candidate(
                    "헬스", "헬스", ProgramCategory.FITNESS_STRENGTH, AdultEligibility.UNKNOWN, 1),
                candidate("요가", "요가", ProgramCategory.YOGA_PILATES, AdultEligibility.UNKNOWN, 4),
                candidate("농구", "농구", ProgramCategory.BALL_SPORTS, AdultEligibility.UNKNOWN, 16),
                candidate(
                    "태권도", "태권도", ProgramCategory.MARTIAL_ARTS, AdultEligibility.ADULT_EXPLICIT, 1),
                candidate(
                    "기타 운동", "기타", ProgramCategory.OTHER, AdultEligibility.ADULT_EXPLICIT, 1)));

    SimpleRecommendationResult result =
        simpleRecommendationService.recommend(
            new SimpleRecommendationRequest(
                FitnessGoal.CARDIO_ENDURANCE,
                ActivityLevel.LOW,
                ExperienceLevel.RETURNING,
                "11",
                "11000",
                Set.of(Weekday.MON, Weekday.WED, Weekday.FRI),
                Set.of(ProgramCategory.SWIMMING_AQUA),
                Set.of(ProgramCategory.MARTIAL_ARTS)));

    assertThat(result.publicFacilityProgramDataVersion()).isEqualTo("202608");
    assertThat(result.searchScope()).isEqualTo(SearchScope.SIDO_FALLBACK);
    assertThat(result.recommendations()).hasSize(5);
    assertThat(result.recommendations())
        .extracting(SimpleRecommendationResult.RecommendedProgram::category)
        .doesNotContain(ProgramCategory.MARTIAL_ARTS, ProgramCategory.OTHER);
    assertThat(result.recommendations().getFirst())
        .extracting(
            SimpleRecommendationResult.RecommendedProgram::programName,
            SimpleRecommendationResult.RecommendedProgram::score,
            SimpleRecommendationResult.RecommendedProgram::reasons)
        .containsExactly(
            "초급 수영",
            new BigDecimal("100.00"),
            List.of(
                "심폐지구력 목표에 맞는 종목이에요.",
                "초보자도 시작하기 좋은 프로그램이에요.",
                "선호 종목이에요.",
                "성인 대상이 명확해요.",
                "가능한 요일 중 월·수에 참여할 수 있어요."));
    verify(programRepository).findRecommendationCandidatesBySido("11", 1L, LocalDate.now(), 21);
  }

  @Test
  void returnsEmptyRecommendationsWhenNoCompletedProgramImportExists() {
    when(dataImportRepository.findFirstBySourceTypeAndStatusOrderByCompletedAtDescIdDesc(
            DataImportSourceType.PUBLIC_FACILITY_PROGRAM, DataImportStatus.COMPLETED))
        .thenReturn(Optional.empty());

    SimpleRecommendationResult result =
        simpleRecommendationService.recommend(
            new SimpleRecommendationRequest(
                FitnessGoal.STRENGTH,
                ActivityLevel.LOW,
                ExperienceLevel.BEGINNER,
                "11",
                null,
                Set.of(Weekday.MON),
                Set.of(),
                Set.of()));

    assertThat(result.publicFacilityProgramDataVersion()).isNull();
    assertThat(result.searchScope()).isEqualTo(SearchScope.SIDO);
    assertThat(result.recommendations()).isEmpty();
    verifyNoInteractions(programRepository);
  }

  @Test
  void excludesAvoidedCategoriesWhenSearchingBySido() {
    DataImport dataImport = completedImport();
    when(dataImportRepository.findFirstBySourceTypeAndStatusOrderByCompletedAtDescIdDesc(
            DataImportSourceType.PUBLIC_FACILITY_PROGRAM, DataImportStatus.COMPLETED))
        .thenReturn(Optional.of(dataImport));
    when(programRepository.findRecommendationCandidatesBySido(
            eq("11"), eq(1L), any(LocalDate.class), eq(1)))
        .thenReturn(
            List.of(
                candidate("태권도", "태권도", ProgramCategory.MARTIAL_ARTS, AdultEligibility.UNKNOWN, 1),
                candidate("수영", "수영", ProgramCategory.SWIMMING_AQUA, AdultEligibility.UNKNOWN, 1)));

    SimpleRecommendationResult result =
        simpleRecommendationService.recommend(
            new SimpleRecommendationRequest(
                FitnessGoal.STRENGTH,
                ActivityLevel.LOW,
                ExperienceLevel.BEGINNER,
                "11",
                null,
                Set.of(Weekday.MON),
                Set.of(),
                Set.of(ProgramCategory.MARTIAL_ARTS)));

    assertThat(result.searchScope()).isEqualTo(SearchScope.SIDO);
    assertThat(result.recommendations())
        .extracting(SimpleRecommendationResult.RecommendedProgram::category)
        .containsExactly(ProgramCategory.SWIMMING_AQUA);
  }

  private DataImport completedImport() {
    DataImport dataImport =
        DataImport.startPublicFacilityProgram("202608", "program.csv", "a".repeat(64));
    dataImport.complete(1, 1, 0);
    ReflectionTestUtils.setField(dataImport, "id", 1L);
    return dataImport;
  }

  private RecommendationCandidateProjection candidate(
      String name,
      String typeName,
      ProgramCategory category,
      AdultEligibility adultEligibility,
      int weekdayMask) {
    return new Candidate(
        1L,
        name,
        typeName,
        category.name(),
        "체육관",
        "서울시",
        LocalDate.now(),
        LocalDate.now().plusDays(30),
        "월수",
        (byte) weekdayMask,
        new BigDecimal("24000"),
        "월",
        "https://fitdaero.kr",
        adultEligibility.name());
  }

  private record Candidate(
      Long programId,
      String programName,
      String typeName,
      String programCategory,
      String facilityName,
      String address,
      LocalDate startsOn,
      LocalDate endsOn,
      String weekdayText,
      Byte weekdayMask,
      BigDecimal price,
      String priceTypeName,
      String homepageUrl,
      String adultEligibility)
      implements RecommendationCandidateProjection {

    @Override
    public Long getProgramId() {
      return programId;
    }

    @Override
    public String getProgramName() {
      return programName;
    }

    @Override
    public String getTypeName() {
      return typeName;
    }

    @Override
    public String getProgramCategory() {
      return programCategory;
    }

    @Override
    public String getFacilityName() {
      return facilityName;
    }

    @Override
    public String getAddress() {
      return address;
    }

    @Override
    public LocalDate getStartsOn() {
      return startsOn;
    }

    @Override
    public LocalDate getEndsOn() {
      return endsOn;
    }

    @Override
    public String getWeekdayText() {
      return weekdayText;
    }

    @Override
    public Byte getWeekdayMask() {
      return weekdayMask;
    }

    @Override
    public BigDecimal getPrice() {
      return price;
    }

    @Override
    public String getPriceTypeName() {
      return priceTypeName;
    }

    @Override
    public String getHomepageUrl() {
      return homepageUrl;
    }

    @Override
    public String getAdultEligibility() {
      return adultEligibility;
    }
  }
}
