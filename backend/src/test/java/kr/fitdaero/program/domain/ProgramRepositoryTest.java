package kr.fitdaero.program.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.fitdaero.TestcontainersConfig;
import kr.fitdaero.dataimport.domain.DataImport;
import kr.fitdaero.dataimport.domain.DataImportRepository;
import kr.fitdaero.dataimport.domain.DataImportSourceType;
import kr.fitdaero.dataimport.domain.DataImportStatus;
import kr.fitdaero.facility.domain.Facility;
import kr.fitdaero.facility.domain.FacilityRepository;
import kr.fitdaero.recommendation.RecommendationCandidateProjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
class ProgramRepositoryTest {

  @Autowired private DataImportRepository dataImportRepository;

  @Autowired private FacilityRepository facilityRepository;

  @Autowired private ProgramRepository programRepository;

  @Test
  void savesProgramWithFacilityAndImport() {
    DataImport dataImport =
        dataImportRepository.save(
            DataImport.startPublicFacilityProgram("202608", "program.csv", "a".repeat(64)));
    Facility facility =
        facilityRepository.save(
            Facility.create(
                "b".repeat(64), "핏대로체육관", "11", "서울특별시", "11000", "종로구", "서울특별시 종로구 운동로 1"));

    programRepository.saveAndFlush(
        Program.create(
            facility,
            dataImport,
            "c".repeat(64),
            "성인 수영",
            "성인",
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 31),
            "월",
            "10:00",
            ProgramCategory.SWIMMING_AQUA,
            AdultEligibility.ADULT_EXPLICIT,
            "NORMALIZED"));

    assertThat(programRepository.count()).isEqualTo(1);
  }

  @Test
  void findsOnlyEligibleCandidatesInSigungu() {
    LocalDate today = LocalDate.now();
    DataImport dataImport = completedImport("candidate", "d".repeat(64), LocalDateTime.now());
    DataImport otherImport =
        completedImport("other-candidate", "p".repeat(64), LocalDateTime.now().minusMinutes(1));
    Facility localFacility = facility("e".repeat(64), "11000");

    program(
        dataImport, localFacility, "eligible", today, (byte) 1, AdultEligibility.ADULT_EXPLICIT);
    program(
        dataImport,
        facility("f".repeat(64), "12000"),
        "other-sigungu",
        today,
        (byte) 1,
        AdultEligibility.ADULT_EXPLICIT);
    program(
        otherImport,
        localFacility,
        "other-import",
        today,
        (byte) 1,
        AdultEligibility.ADULT_EXPLICIT);
    program(
        dataImport,
        localFacility,
        "ended",
        today.minusDays(1),
        (byte) 1,
        AdultEligibility.ADULT_EXPLICIT);
    program(dataImport, localFacility, "child", today, (byte) 1, AdultEligibility.CHILD_ONLY);
    program(
        dataImport,
        localFacility,
        "wrong-weekday",
        today,
        (byte) 2,
        AdultEligibility.ADULT_EXPLICIT);
    program(
        dataImport, localFacility, "unknown-weekday", today, null, AdultEligibility.ADULT_EXPLICIT);

    List<RecommendationCandidateProjection> candidates =
        programRepository.findRecommendationCandidatesBySidoAndSigungu(
            "11", "11000", dataImport.getId(), today, 1);

    assertThat(candidates).hasSize(1);
    RecommendationCandidateProjection candidate = candidates.getFirst();
    assertThat(candidate.getProgramName()).isEqualTo("eligible");
    assertThat(candidate.getTypeName()).isEqualTo("수영");
    assertThat(candidate.getFacilityName()).isEqualTo("핏대로체육관");
    assertThat(candidate.getAddress()).isEqualTo("서울특별시 종로구 운동로 1");
    assertThat(candidate.getProgramCategory()).isEqualTo(ProgramCategory.SWIMMING_AQUA.name());
    assertThat(candidate.getAdultEligibility()).isEqualTo(AdultEligibility.ADULT_EXPLICIT.name());
    assertThat(candidate.getWeekdayMask()).isEqualTo((byte) 1);
    assertThat(candidate.getPrice()).isEqualByComparingTo("24000");
  }

  @Test
  void findsEligibleCandidatesAcrossSido() {
    LocalDate today = LocalDate.now();
    DataImport dataImport = completedImport("sido", "g".repeat(64), LocalDateTime.now());

    program(
        dataImport,
        facility("h".repeat(64), "11000"),
        "jongno",
        today,
        (byte) 1,
        AdultEligibility.ADULT_EXPLICIT);
    program(
        dataImport,
        facility("i".repeat(64), "12000"),
        "jung",
        today,
        (byte) 1,
        AdultEligibility.ADULT_POSSIBLE);
    program(
        dataImport,
        facility("j".repeat(64), "26", "21000"),
        "busan",
        today,
        (byte) 1,
        AdultEligibility.ADULT_EXPLICIT);

    assertThat(
            programRepository.findRecommendationCandidatesBySido(
                "11", dataImport.getId(), today, 1))
        .extracting(RecommendationCandidateProjection::getProgramName)
        .containsExactlyInAnyOrder("jongno", "jung");
  }

  @Test
  void findsLatestCompletedPublicProgramImport() {
    LocalDateTime completedAt = LocalDateTime.of(2026, 8, 20, 12, 0);
    completedImport("older", "k".repeat(64), completedAt.minusMinutes(1));
    completedImport("same-time-first", "l".repeat(64), completedAt);
    completedImport("same-time-last", "m".repeat(64), completedAt);

    DataImport failed =
        DataImport.startPublicFacilityProgram("failed", "failed.csv", "n".repeat(64));
    failed.fail("failure");
    ReflectionTestUtils.setField(failed, "completedAt", completedAt.plusMinutes(1));
    dataImportRepository.saveAndFlush(failed);

    DataImport running =
        DataImport.startPublicFacilityProgram("running", "running.csv", "o".repeat(64));
    dataImportRepository.saveAndFlush(running);

    DataImport latest =
        dataImportRepository
            .findFirstBySourceTypeAndStatusOrderByCompletedAtDescIdDesc(
                DataImportSourceType.PUBLIC_FACILITY_PROGRAM, DataImportStatus.COMPLETED)
            .orElseThrow();

    assertThat(latest.getDataVersion()).isEqualTo("same-time-last");
  }

  private DataImport completedImport(
      String dataVersion, String checksum, LocalDateTime completedAt) {
    DataImport dataImport =
        DataImport.startPublicFacilityProgram(dataVersion, dataVersion + ".csv", checksum);
    dataImport.complete(1, 1, 0);
    ReflectionTestUtils.setField(dataImport, "completedAt", completedAt);
    return dataImportRepository.saveAndFlush(dataImport);
  }

  private Facility facility(String sourceKey, String sigunguCode) {
    return facility(sourceKey, "11", sigunguCode);
  }

  private Facility facility(String sourceKey, String sidoCode, String sigunguCode) {
    return facilityRepository.saveAndFlush(
        Facility.create(
            sourceKey, "핏대로체육관", sidoCode, "서울특별시", sigunguCode, "시군구", "서울특별시 종로구 운동로 1"));
  }

  private void program(
      DataImport dataImport,
      Facility facility,
      String name,
      LocalDate endsOn,
      Byte weekdayMask,
      AdultEligibility adultEligibility) {
    programRepository.saveAndFlush(
        Program.create(
            facility,
            dataImport,
            name + "-source-key",
            "수영",
            name,
            "성인",
            endsOn.minusDays(1),
            endsOn,
            "월",
            weekdayMask,
            "10:00",
            25,
            new BigDecimal("24000"),
            "월",
            ProgramCategory.SWIMMING_AQUA,
            adultEligibility,
            "NORMALIZED"));
  }
}
