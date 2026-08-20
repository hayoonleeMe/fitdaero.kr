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
import java.util.ArrayList;
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

    assertThat(importer.importFile(first)).isEqualTo(ProgramCsvImporter.ImportResult.COMPLETED);

    DataImport firstImport = dataImportRepository.findAll().getFirst();
    assertThat(firstImport.getDataVersion()).isEqualTo("program");
    assertThat(firstImport.getStatus()).isEqualTo(DataImportStatus.COMPLETED);
    assertThat(firstImport.getTotalCount()).isEqualTo(1);
    assertThat(firstImport.getSuccessCount()).isEqualTo(1);
    assertThat(firstImport.getFailureCount()).isZero();
    assertThat(facilityRepository.count()).isEqualTo(1);
    assertThat(programRepository.count()).isEqualTo(1);

    assertThat(importer.importFile(first)).isEqualTo(ProgramCsvImporter.ImportResult.SKIPPED);
    assertThat(dataImportRepository.count()).isEqualTo(1);

    assertThat(importer.importFile(csv("program-updated.csv", row("성인 수영", "30000"))))
        .isEqualTo(ProgramCsvImporter.ImportResult.COMPLETED);
    assertThat(dataImportRepository.count()).isEqualTo(2);
    assertThat(facilityRepository.count()).isEqualTo(1);
    assertThat(programRepository.count()).isEqualTo(1);
    assertThat(programRepository.findAll().getFirst().getPrice())
        .isEqualByComparingTo(new BigDecimal("30000"));
  }

  @Test
  void importsUtf8BomCsv() throws IOException {
    assertThat(importer.importFile(csvWithBom("bom.csv", row("성인 수영", "24000"))))
        .isEqualTo(ProgramCsvImporter.ImportResult.COMPLETED);

    assertThat(dataImportRepository.findAll().getFirst().getStatus())
        .isEqualTo(DataImportStatus.COMPLETED);
    assertThat(programRepository.count()).isEqualTo(1);
  }

  @Test
  void importsBackslashEscapedQuotes() throws IOException {
    Path csvFile = csv("backslash-escaped.csv", row("placeholder", "24000"));
    Files.writeString(
        csvFile,
        Files.readString(csvFile, StandardCharsets.UTF_8)
            .replace("\"placeholder\"", "\"\\\" (화목)\""),
        StandardCharsets.UTF_8);

    assertThat(importer.importFile(csvFile)).isEqualTo(ProgramCsvImporter.ImportResult.COMPLETED);

    assertThat(dataImportRepository.findAll().getFirst().getStatus())
        .isEqualTo(DataImportStatus.COMPLETED);
    assertThat(programRepository.count()).isEqualTo(1);
  }

  @Test
  void retriesFailedImportWithTheSameChecksum() throws IOException {
    Path csvFile = csv("retry.csv", row("성인 수영", "24000"));
    DataImport failed =
        DataImport.startPublicFacilityProgram("retry", "retry.csv", checksum(csvFile));
    failed.fail("InterruptedException");
    dataImportRepository.saveAndFlush(failed);

    assertThat(importer.importFile(csvFile)).isEqualTo(ProgramCsvImporter.ImportResult.COMPLETED);

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

    assertThat(importer.importFile(missingHeaders))
        .isEqualTo(ProgramCsvImporter.ImportResult.FAILED);
    assertThat(dataImportRepository.findAll().getFirst().getStatus())
        .isEqualTo(DataImportStatus.FAILED);

    cleanUp();
    assertThat(importer.importFile(csv("invalid.csv", row("", "24000"))))
        .isEqualTo(ProgramCsvImporter.ImportResult.FAILED);

    DataImport invalidRows = dataImportRepository.findAll().getFirst();
    assertThat(invalidRows.getStatus()).isEqualTo(DataImportStatus.FAILED);
    assertThat(invalidRows.getTotalCount()).isEqualTo(1);
    assertThat(invalidRows.getFailureCount()).isEqualTo(1);
  }

  @Test
  void keepsCompletedProgramsWhenNewImportFailsDuringDatabaseWrite() throws IOException {
    importer.importFile(csv("completed.csv", row("기존 수영", "24000")));

    assertThat(
            importer.importFile(
                csv("broken.csv", row("새 수영", "24000"), row("n".repeat(256), "24000"))))
        .isEqualTo(ProgramCsvImporter.ImportResult.FAILED);

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

  @Test
  void batchesLookupsAndKeepsTheLastValueForDuplicateProgramKeys() throws IOException {
    List<List<String>> rows = new ArrayList<>();
    rows.add(row("같은 프로그램", "10000"));
    rows.add(row("같은 프로그램", "20000"));
    for (int index = 1; index < 500; index++) {
      rows.add(row("프로그램 " + index, "10000"));
    }

    importer.importFile(csv("batch.csv", rows));

    assertThat(programRepository.count()).isEqualTo(500);
    assertThat(
            programRepository.findAll().stream()
                .filter(program -> program.getPrice().compareTo(new BigDecimal("20000")) == 0)
                .count())
        .isEqualTo(1);
  }

  @SafeVarargs
  private final Path csv(String fileName, List<String>... rows) throws IOException {
    return csv(fileName, false, List.of(rows));
  }

  @SafeVarargs
  private final Path csvWithBom(String fileName, List<String>... rows) throws IOException {
    return csv(fileName, true, List.of(rows));
  }

  private Path csv(String fileName, List<List<String>> rows) throws IOException {
    return csv(fileName, false, rows);
  }

  private Path csv(String fileName, boolean withBom, List<List<String>> rows) throws IOException {
    Path csvFile = tempDir.resolve(fileName);
    try (Writer writer = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8)) {
      if (withBom) {
        writer.write('\uFEFF');
      }
      try (CSVPrinter printer =
          new CSVPrinter(
              writer,
              CSVFormat.DEFAULT.builder().setHeader(HEADERS.toArray(String[]::new)).get())) {
        for (List<String> row : rows) {
          printer.printRecord(row);
        }
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
