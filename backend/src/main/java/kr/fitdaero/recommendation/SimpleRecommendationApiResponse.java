package kr.fitdaero.recommendation;

import java.util.List;

public record SimpleRecommendationApiResponse(
    String analysisType,
    String analysisSummary,
    DataVersions dataVersions,
    SearchScope searchScope,
    List<SimpleRecommendationResult.RecommendedProgram> recommendations) {

  private static final String SIMPLE = "SIMPLE";
  private static final String ANALYSIS_SUMMARY = "선택한 목표와 생활 응답을 바탕으로 추천했어요.";

  public static SimpleRecommendationApiResponse from(SimpleRecommendationResult result) {
    return new SimpleRecommendationApiResponse(
        SIMPLE,
        ANALYSIS_SUMMARY,
        new DataVersions(result.publicFacilityProgramDataVersion()),
        result.searchScope(),
        result.recommendations());
  }

  public record DataVersions(String publicFacilityProgram) {}
}
