package kr.fitdaero.program.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import kr.fitdaero.TestcontainersConfig;
import kr.fitdaero.dataimport.domain.DataImport;
import kr.fitdaero.dataimport.domain.DataImportRepository;
import kr.fitdaero.facility.domain.Facility;
import kr.fitdaero.facility.domain.FacilityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

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
}
