package kr.fitdaero.program.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import kr.fitdaero.TestcontainersConfig;
import kr.fitdaero.dataimport.domain.DataImport;
import kr.fitdaero.dataimport.domain.DataImportRepository;
import kr.fitdaero.dataimport.domain.DataImportSourceType;
import kr.fitdaero.dataimport.domain.DataImportStatus;
import kr.fitdaero.facility.domain.FacilityRepository;
import kr.fitdaero.program.domain.ProgramRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class ProgramCsvImporterTest {

  private static final List<String> HEADERS =
      List.of(
          "FCLTY_NM",
          "FCLTY_ADDR",
          "CTPRVN_CD",
          "CTPRVN_NM",
          "SIGNGU_CD",
          "SIGNGU_NM",
          "EMD_NM",
          "FCLTY_LA",
          "FCLTY_LO",
          "FCLTY_TEL_NO",
          "HMPG_URL",
          "PROGRM_TY_NM",
          "PROGRM_NM",
          "PROGRM_TRGET_NM",
          "PROGRM_BEGIN_DE",
          "PROGRM_END_DE",
          "PROGRM_ESTBL_WKDAY_NM",
          "PROGRM_ESTBL_TIZN_VALUE",
          "PROGRM_RCRIT_NMPR_CO",
          "PROGRM_PRC",
          "PROGRM_PRC_TY_NM");

  @Autowired private ProgramCsvImporter importer;

  @Autowired private DataImportRepository dataImportRepository;

  @Autowired private FacilityRepository facilityRepository;

  @Autowired private ProgramRepository programRepository;

  @TempDir Path tempDir;

  @AfterEach
  void cleanUp() {
    programRepository.deleteAll();
    facilityRepository.deleteAll();
    dataImportRepository.deleteAll();
  }

  @Test
  void importsQuotedRowsAndUpsertsExistingPrograms() throws IOException {
    Path first = csv("program.csv", row("성인 수영", "24000.00000"));

    importer.importFile(first);

    DataImport firstImport = dataImportRepository.findAll().getFirst();
    assertThat(firstImport.getDataVersion()).isEqualTo("program");
    assertThat(firstImport.getStatus()).isEqualTo(DataImportStatus.COMPLETED);
    assertThat(firstImport.getTotalCount()).isEqualTo(1);
    assertThat(firstImport.getSuccessCount()).isEqualTo(1);
    assertThat(firstImport.getFailureCount()).isZero();
    assertThat(facilityRepository.count()).isEqualTo(1);
    assertThat(programRepository.count()).isEqualTo(1);

    importer.importFile(first);
    assertThat(dataImportRepository.count()).isEqualTo(1);

    importer.importFile(csv("program-updated.csv", row("성인 수영", "30000")));
    assertThat(dataImportRepository.count()).isEqualTo(2);
    assertThat(facilityRepository.count()).isEqualTo(1);
    assertThat(programRepository.count()).isEqualTo(1);
    assertThat(programRepository.findAll().getFirst().getPrice())
        .isEqualByComparingTo(new BigDecimal("30000"));
  }

  @Test
  void retriesFailedImportWithTheSameChecksum() throws IOException {
    Path csvFile = csv("retry.csv", row("성인 수영", "24000"));
    DataImport failed =
        DataImport.startPublicFacilityProgram("retry", "retry.csv", checksum(csvFile));
    failed.fail("InterruptedException");
    dataImportRepository.saveAndFlush(failed);

    importer.importFile(csvFile);

    DataImport retried =
        dataImportRepository
            .findBySourceTypeAndFileChecksum(
                DataImportSourceType.PUBLIC_FACILITY_PROGRAM, checksum(csvFile))
            .orElseThrow();
    assertThat(retried.getStatus()).isEqualTo(DataImportStatus.COMPLETED);
    assertThat(programRepository.count()).isEqualTo(1);
  }

  @Test
  void marksMissingHeadersAndRowsWithoutValidProgramsAsFailed() throws IOException {
    Path missingHeaders = tempDir.resolve("missing.csv");
    Files.writeString(missingHeaders, "FCLTY_NM,PROGRM_NM\n체육관,수영\n", StandardCharsets.UTF_8);

    importer.importFile(missingHeaders);
    assertThat(dataImportRepository.findAll().getFirst().getStatus())
        .isEqualTo(DataImportStatus.FAILED);

    cleanUp();
    importer.importFile(csv("invalid.csv", row("", "24000")));

    DataImport invalidRows = dataImportRepository.findAll().getFirst();
    assertThat(invalidRows.getStatus()).isEqualTo(DataImportStatus.FAILED);
    assertThat(invalidRows.getTotalCount()).isEqualTo(1);
    assertThat(invalidRows.getFailureCount()).isEqualTo(1);
  }

  @Test
  void keepsCompletedProgramsWhenNewImportFailsDuringDatabaseWrite() throws IOException {
    importer.importFile(csv("completed.csv", row("기존 수영", "24000")));

    importer.importFile(csv("broken.csv", row("새 수영", "24000"), row("n".repeat(256), "24000")));

    assertThat(programRepository.count()).isEqualTo(1);
    assertThat(dataImportRepository.count()).isEqualTo(2);
    assertThat(
            dataImportRepository.findAll().stream()
                .filter(importHistory -> importHistory.getDataVersion().equals("broken"))
                .findFirst()
                .orElseThrow()
                .getStatus())
        .isEqualTo(DataImportStatus.FAILED);
  }

  @SafeVarargs
  private final Path csv(String fileName, List<String>... rows) throws IOException {
    Path csvFile = tempDir.resolve(fileName);
    try (Writer writer = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8);
        CSVPrinter printer =
            new CSVPrinter(
                writer,
                CSVFormat.DEFAULT.builder().setHeader(HEADERS.toArray(String[]::new)).get())) {
      for (List<String> row : rows) {
        printer.printRecord(row);
      }
    }
    return csvFile;
  }

  private List<String> row(String programName, String price) {
    return List.of(
        "핏대로 체육관",
        "서울특별시 종로구 운동로 1",
        "11",
        "서울특별시",
        "11000",
        "종로구",
        "청운동",
        "37.5518979414195",
        "127.020752859362",
        "0222047900",
        "https://fitdaero.kr",
        "수영",
        programName,
        "성인,<br>청소년",
        "20260801",
        "20260831",
        "월수금",
        "10:00",
        "25",
        price,
        "월");
  }

  private String checksum(Path csvFile) throws IOException {
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(csvFile)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
