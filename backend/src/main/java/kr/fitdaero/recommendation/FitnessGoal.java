package kr.fitdaero.recommendation;

import java.util.Set;
import kr.fitdaero.program.domain.ProgramCategory;

public enum FitnessGoal {
  STRENGTH(
      "근력",
      ProgramCategory.SWIMMING_AQUA,
      ProgramCategory.FITNESS_STRENGTH,
      ProgramCategory.YOGA_PILATES,
      ProgramCategory.RACKET_SPORTS,
      ProgramCategory.BALL_SPORTS,
      ProgramCategory.MARTIAL_ARTS,
      ProgramCategory.CLIMBING),
  MUSCULAR_ENDURANCE(
      "근지구력",
      ProgramCategory.FITNESS_STRENGTH,
      ProgramCategory.MARTIAL_ARTS,
      ProgramCategory.CLIMBING),
  FLEXIBILITY(
      "유연성",
      ProgramCategory.SWIMMING_AQUA,
      ProgramCategory.YOGA_PILATES,
      ProgramCategory.DANCE_AEROBIC,
      ProgramCategory.MARTIAL_ARTS,
      ProgramCategory.CLIMBING,
      ProgramCategory.GOLF),
  CARDIO_ENDURANCE(
      "심폐지구력",
      ProgramCategory.SWIMMING_AQUA,
      ProgramCategory.CARDIO,
      ProgramCategory.DANCE_AEROBIC,
      ProgramCategory.RACKET_SPORTS,
      ProgramCategory.BALL_SPORTS),
  WEIGHT_MANAGEMENT(
      "체중관리",
      ProgramCategory.SWIMMING_AQUA,
      ProgramCategory.FITNESS_STRENGTH,
      ProgramCategory.CARDIO,
      ProgramCategory.DANCE_AEROBIC),
  STRESS_RELIEF(
      "스트레스 해소",
      ProgramCategory.YOGA_PILATES,
      ProgramCategory.DANCE_AEROBIC,
      ProgramCategory.RACKET_SPORTS,
      ProgramCategory.BALL_SPORTS,
      ProgramCategory.MARTIAL_ARTS,
      ProgramCategory.GOLF);

  private final String label;
  private final Set<ProgramCategory> categories;

  FitnessGoal(String label, ProgramCategory... categories) {
    this.label = label;
    this.categories = Set.of(categories);
  }

  public String label() {
    return label;
  }

  public boolean matches(ProgramCategory category) {
    return categories.contains(category);
  }
}
