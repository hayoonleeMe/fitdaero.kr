package kr.fitdaero.recommendation;

import java.util.Set;
import kr.fitdaero.program.domain.ProgramCategory;

public record SimpleRecommendationRequest(
    FitnessGoal goal,
    ActivityLevel activityLevel,
    ExperienceLevel experienceLevel,
    String sidoCode,
    String sigunguCode,
    Set<Weekday> weekdays,
    Set<ProgramCategory> preferredCategories,
    Set<ProgramCategory> avoidedCategories) {}
