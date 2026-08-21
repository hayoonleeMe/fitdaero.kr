package kr.fitdaero.program.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import kr.fitdaero.recommendation.RecommendationCandidateProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgramRepository extends JpaRepository<Program, Long> {

  String RECOMMENDATION_CANDIDATE_SELECT =
      """
      SELECT p.id AS programId,
             p.name AS programName,
             p.type_name AS typeName,
             p.program_category AS programCategory,
             f.name AS facilityName,
             f.address AS address,
             p.starts_on AS startsOn,
             p.ends_on AS endsOn,
             p.weekday_text AS weekdayText,
             p.weekday_mask AS weekdayMask,
             p.price AS price,
             p.price_type_name AS priceTypeName,
             f.homepage_url AS homepageUrl,
             p.adult_eligibility AS adultEligibility
      FROM program p
      JOIN facility f ON f.id = p.facility_id
      WHERE f.sido_code = :sidoCode
        AND p.import_id = :importId
        AND p.ends_on >= :today
        AND p.weekday_mask IS NOT NULL
        AND (p.weekday_mask & :availableWeekdayMask) <> 0
        AND p.adult_eligibility <> 'CHILD_ONLY'
      """;

  @Query(
      value = RECOMMENDATION_CANDIDATE_SELECT + "AND f.sigungu_code = :sigunguCode",
      nativeQuery = true)
  List<RecommendationCandidateProjection> findRecommendationCandidatesBySidoAndSigungu(
      @Param("sidoCode") String sidoCode,
      @Param("sigunguCode") String sigunguCode,
      @Param("importId") Long importId,
      @Param("today") LocalDate today,
      @Param("availableWeekdayMask") int availableWeekdayMask);

  @Query(value = RECOMMENDATION_CANDIDATE_SELECT, nativeQuery = true)
  List<RecommendationCandidateProjection> findRecommendationCandidatesBySido(
      @Param("sidoCode") String sidoCode,
      @Param("importId") Long importId,
      @Param("today") LocalDate today,
      @Param("availableWeekdayMask") int availableWeekdayMask);

  List<Program> findBySourceKeyIn(Collection<String> sourceKeys);
}
