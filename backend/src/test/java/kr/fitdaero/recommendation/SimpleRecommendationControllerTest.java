package kr.fitdaero.recommendation;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import kr.fitdaero.program.domain.ProgramCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SimpleRecommendationController.class)
class SimpleRecommendationControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private SimpleRecommendationService simpleRecommendationService;

  @Test
  void returnsSimpleRecommendationResponse() throws Exception {
    when(simpleRecommendationService.recommend(
            new SimpleRecommendationRequest(
                FitnessGoal.CARDIO_ENDURANCE,
                ActivityLevel.LOW,
                ExperienceLevel.BEGINNER,
                "11",
                "11200",
                Set.of(Weekday.MON, Weekday.WED),
                Set.of(ProgramCategory.SWIMMING_AQUA),
                Set.of(ProgramCategory.MARTIAL_ARTS))))
        .thenReturn(
            new SimpleRecommendationResult(
                "KS_PUBLIC_ALSFC_PROGRM_INFO_202606",
                SearchScope.SIGUNGU,
                List.of(
                    new SimpleRecommendationResult.RecommendedProgram(
                        1L,
                        "성인 초급 수영",
                        ProgramCategory.SWIMMING_AQUA,
                        "금호교육문화관수영장",
                        "서울특별시 성동구",
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31),
                        "월수",
                        BigDecimal.valueOf(49500),
                        null,
                        "https://example.com",
                        BigDecimal.valueOf(82.5),
                        List.of("심폐지구력 목표에 맞는 종목이에요.")))));

    mockMvc
        .perform(
            post("/api/recommendations/simple")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.analysisType").value("SIMPLE"))
        .andExpect(jsonPath("$.analysisSummary").value("선택한 목표와 생활 응답을 바탕으로 추천했어요."))
        .andExpect(
            jsonPath("$.dataVersions.publicFacilityProgram")
                .value("KS_PUBLIC_ALSFC_PROGRM_INFO_202606"))
        .andExpect(jsonPath("$.searchScope").value("SIGUNGU"))
        .andExpect(jsonPath("$.recommendations[0].programId").value(1))
        .andExpect(jsonPath("$.recommendations[0].category").value("SWIMMING_AQUA"));

    verify(simpleRecommendationService)
        .recommend(
            new SimpleRecommendationRequest(
                FitnessGoal.CARDIO_ENDURANCE,
                ActivityLevel.LOW,
                ExperienceLevel.BEGINNER,
                "11",
                "11200",
                Set.of(Weekday.MON, Weekday.WED),
                Set.of(ProgramCategory.SWIMMING_AQUA),
                Set.of(ProgramCategory.MARTIAL_ARTS)));
  }

  @Test
  void defaultsOptionalCategoriesAndTreatsMissingSigunguCodeAsSidoSearch() throws Exception {
    when(simpleRecommendationService.recommend(
            new SimpleRecommendationRequest(
                FitnessGoal.CARDIO_ENDURANCE,
                ActivityLevel.LOW,
                ExperienceLevel.BEGINNER,
                "11",
                null,
                Set.of(Weekday.MON),
                Set.of(),
                Set.of())))
        .thenReturn(new SimpleRecommendationResult("version", SearchScope.SIDO, List.of()));

    mockMvc
        .perform(
            post("/api/recommendations/simple")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "goal": "CARDIO_ENDURANCE",
                      "activityLevel": "LOW",
                      "experienceLevel": "BEGINNER",
                      "sidoCode": "11",
                      "weekdays": ["MON"]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.searchScope").value("SIDO"));

    verify(simpleRecommendationService)
        .recommend(
            new SimpleRecommendationRequest(
                FitnessGoal.CARDIO_ENDURANCE,
                ActivityLevel.LOW,
                ExperienceLevel.BEGINNER,
                "11",
                null,
                Set.of(Weekday.MON),
                Set.of(),
                Set.of()));
  }

  @Test
  void returnsValidationErrorWhenRequiredFieldsAreMissing() throws Exception {
    mockMvc
        .perform(
            post("/api/recommendations/simple")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(
            jsonPath("$.fieldErrors[*].field")
                .value(
                    org.hamcrest.Matchers.hasItems(
                        "goal", "activityLevel", "experienceLevel", "sidoCode", "weekdays")));
  }

  @Test
  void returnsValidationErrorWhenWeekdaysAreEmpty() throws Exception {
    mockMvc
        .perform(
            post("/api/recommendations/simple")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest().replace("[\"MON\", \"WED\"]", "[]")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("weekdays"));
  }

  @Test
  void returnsValidationErrorWhenRegionCodeLengthIsInvalid() throws Exception {
    mockMvc
        .perform(
            post("/api/recommendations/simple")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest().replace("\"sidoCode\": \"11\"", "\"sidoCode\": \"110\"")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("sidoCode"));
  }

  @Test
  void returnsValidationErrorWhenCategoriesOverlap() throws Exception {
    mockMvc
        .perform(
            post("/api/recommendations/simple")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest().replace("[\"MARTIAL_ARTS\"]", "[\"SWIMMING_AQUA\"]")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].message").value("선호 종목과 비선호 종목은 겹칠 수 없습니다."));
  }

  @Test
  void returnsValidationErrorWhenOtherCategoryIsSelected() throws Exception {
    mockMvc
        .perform(
            post("/api/recommendations/simple")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest().replace("[\"SWIMMING_AQUA\"]", "[\"OTHER\"]")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].message").value("OTHER는 선호 또는 비선호 종목으로 선택할 수 없습니다."));
  }

  @Test
  void returnsValidationErrorWhenEnumIsInvalid() throws Exception {
    mockMvc
        .perform(
            post("/api/recommendations/simple")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest().replace("CARDIO_ENDURANCE", "RUNNING")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("request"));
  }

  private String validRequest() {
    return """
        {
          "goal": "CARDIO_ENDURANCE",
          "activityLevel": "LOW",
          "experienceLevel": "BEGINNER",
          "sidoCode": "11",
          "sigunguCode": "11200",
          "weekdays": ["MON", "WED"],
          "preferredCategories": ["SWIMMING_AQUA"],
          "avoidedCategories": ["MARTIAL_ARTS"]
        }
        """;
  }
}
