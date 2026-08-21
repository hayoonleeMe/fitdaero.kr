package kr.fitdaero.recommendation;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Set;
import kr.fitdaero.program.domain.ProgramCategory;

public record SimpleRecommendationApiRequest(
    @NotNull(message = "운동 목표는 필수입니다.") FitnessGoal goal,
    @NotNull(message = "활동량은 필수입니다.") ActivityLevel activityLevel,
    @NotNull(message = "운동 경험은 필수입니다.") ExperienceLevel experienceLevel,
    @NotBlank(message = "시도 코드는 필수입니다.")
        @Pattern(regexp = "\\d{2}", message = "시도 코드는 숫자 2자리여야 합니다.")
        String sidoCode,
    @Pattern(regexp = "\\d{5}", message = "시군구 코드는 숫자 5자리여야 합니다.") String sigunguCode,
    @NotEmpty(message = "가능한 요일을 하나 이상 선택해야 합니다.") Set<@NotNull Weekday> weekdays,
    Set<@NotNull ProgramCategory> preferredCategories,
    Set<@NotNull ProgramCategory> avoidedCategories) {

  public SimpleRecommendationApiRequest {
    preferredCategories = preferredCategories == null ? Set.of() : preferredCategories;
    avoidedCategories = avoidedCategories == null ? Set.of() : avoidedCategories;
  }

  @AssertTrue(message = "선호 종목과 비선호 종목은 겹칠 수 없습니다.")
  public boolean hasNoOverlappingCategories() {
    return preferredCategories.stream().noneMatch(avoidedCategories::contains);
  }

  @AssertTrue(message = "OTHER는 선호 또는 비선호 종목으로 선택할 수 없습니다.")
  public boolean hasNoOtherCategory() {
    return !preferredCategories.contains(ProgramCategory.OTHER)
        && !avoidedCategories.contains(ProgramCategory.OTHER);
  }

  public SimpleRecommendationRequest toServiceRequest() {
    return new SimpleRecommendationRequest(
        goal,
        activityLevel,
        experienceLevel,
        sidoCode,
        sigunguCode,
        weekdays,
        preferredCategories,
        avoidedCategories);
  }
}
