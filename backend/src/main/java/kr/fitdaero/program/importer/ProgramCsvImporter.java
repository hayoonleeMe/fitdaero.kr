package kr.fitdaero.program.importer;

import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kr.fitdaero.dataimport.domain.DataImport;
import kr.fitdaero.dataimport.domain.DataImportRepository;
import kr.fitdaero.dataimport.domain.DataImportSourceType;
import kr.fitdaero.dataimport.domain.DataImportStatus;
import kr.fitdaero.facility.domain.Facility;
import kr.fitdaero.facility.domain.FacilityRepository;
import kr.fitdaero.program.domain.Program;
import kr.fitdaero.program.domain.ProgramRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ProgramCsvImporter {

  private static final int BATCH_SIZE = 500;
  private static final List<String> REQUIRED_HEADERS =
      List.of(
          "FCLTY_NM",
          "FCLTY_ADDR",
          "CTPRVN_CD",
          "CTPRVN_NM",
          "SIGNGU_CD",
          "SIGNGU_NM",
          "PROGRM_TY_NM",
          "PROGRM_NM",
          "PROGRM_TRGET_NM",
          "PROGRM_BEGIN_DE",
          "PROGRM_END_DE",
          "PROGRM_ESTBL_WKDAY_NM",
          "PROGRM_ESTBL_TIZN_VALUE",
          "PROGRM_PRC",
          "PROGRM_PRC_TY_NM");

  private final DataImportRepository dataImportRepository;
  private final FacilityRepository facilityRepository;
  private final ProgramRepository programRepository;
  private final EntityManager entityManager;
  private final TransactionTemplate transactionTemplate;

  public ProgramCsvImporter(
      DataImportRepository dataImportRepository,
      FacilityRepository facilityRepository,
      ProgramRepository programRepository,
      EntityManager entityManager,
      PlatformTransactionManager transactionManager) {
    this.dataImportRepository = dataImportRepository;
    this.facilityRepository = facilityRepository;
    this.programRepository = programRepository;
    this.entityManager = entityManager;
    transactionTemplate = new TransactionTemplate(transactionManager);
  }

  public void importFile(Path csvFile) {
    String checksum = checksum(csvFile);
    DataImport dataImport = prepareImport(csvFile, checksum);
    if (dataImport == null) {
      return;
    }

    try {
      Long importId = dataImport.getId();
      transactionTemplate.executeWithoutResult(status -> importRows(csvFile, importId));
    } catch (ImportFailure exception) {
      fail(
          dataImport.getId(),
          exception.totalCount,
          exception.successCount,
          exception.failureCount,
          exception);
    } catch (RuntimeException exception) {
      fail(dataImport.getId(), 0, 0, 0, exception);
    }
  }

  private DataImport prepareImport(Path csvFile, String checksum) {
    Optional<DataImport> existing =
        dataImportRepository.findBySourceTypeAndFileChecksum(
            DataImportSourceType.PUBLIC_FACILITY_PROGRAM, checksum);
    if (existing.isPresent()) {
      DataImport dataImport = existing.get();
      if (dataImport.getStatus() != DataImportStatus.FAILED) {
        return null;
      }
      dataImport.restart();
      return dataImportRepository.saveAndFlush(dataImport);
    }

    String fileName = csvFile.getFileName().toString();
    return dataImportRepository.saveAndFlush(
        DataImport.startPublicFacilityProgram(dataVersion(fileName), fileName, checksum));
  }

  private void importRows(Path csvFile, Long importId) {
    int totalCount = 0;
    int successCount = 0;
    int failureCount = 0;
    DataImport dataImport = dataImportRepository.getReferenceById(importId);

    try (Reader reader = reader(csvFile);
        CSVParser parser =
            CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader)) {
      validateHeaders(parser.getHeaderMap());
      for (CSVRecord record : parser) {
        totalCount++;
        Optional<ProgramCsvRow> row = ProgramCsvRowNormalizer.normalize(record.toMap());
        if (row.isPresent()) {
          upsert(row.get(), dataImport);
          successCount++;
        } else {
          failureCount++;
        }

        if (totalCount % BATCH_SIZE == 0) {
          entityManager.flush();
          entityManager.clear();
          dataImport = dataImportRepository.getReferenceById(importId);
        }
      }
    } catch (IOException exception) {
      throw new ImportFailure(totalCount, successCount, failureCount, exception);
    }

    if (successCount == 0) {
      throw new ImportFailure(totalCount, successCount, failureCount, new IllegalStateException());
    }
    dataImport.complete(totalCount, successCount, failureCount);
  }

  private void validateHeaders(Map<String, Integer> headerMap) {
    if (!headerMap.keySet().containsAll(REQUIRED_HEADERS)) {
      throw new IllegalArgumentException("Required CSV header is missing");
    }
  }

  private void fail(
      Long importId,
      int totalCount,
      int successCount,
      int failureCount,
      RuntimeException exception) {
    dataImportRepository
        .findById(importId)
        .ifPresent(
            importHistory -> {
              importHistory.fail(totalCount, successCount, failureCount, errorSummary(exception));
              dataImportRepository.saveAndFlush(importHistory);
            });
  }

  private String errorSummary(RuntimeException exception) {
    Throwable cause = exception.getCause();
    return (cause == null ? exception : cause).getClass().getSimpleName();
  }

  private void upsert(ProgramCsvRow row, DataImport dataImport) {
    String facilityKey = key(row.facilityName(), row.facilityAddress());
    Facility facility =
        facilityRepository
            .findBySourceKey(facilityKey)
            .map(
                existing -> {
                  updateFacility(existing, row);
                  return existing;
                })
            .orElseGet(() -> facilityRepository.save(createFacility(facilityKey, row)));

    String programKey =
        key(
            facilityKey,
            row.name(),
            row.startsOn().toString(),
            row.endsOn().toString(),
            String.valueOf(row.weekdayMask()),
            row.timeText(),
            row.targetName());
    programRepository
        .findBySourceKey(programKey)
        .ifPresentOrElse(
            existing -> updateProgram(existing, dataImport, row),
            () -> programRepository.save(createProgram(facility, dataImport, programKey, row)));
  }

  private Facility createFacility(String facilityKey, ProgramCsvRow row) {
    return Facility.create(
        facilityKey,
        row.facilityName(),
        row.sidoCode(),
        row.sidoName(),
        row.sigunguCode(),
        row.sigunguName(),
        row.emdName(),
        row.facilityAddress(),
        row.latitude(),
        row.longitude(),
        row.phoneNumber(),
        row.homepageUrl());
  }

  private void updateFacility(Facility facility, ProgramCsvRow row) {
    facility.update(
        row.facilityName(),
        row.sidoCode(),
        row.sidoName(),
        row.sigunguCode(),
        row.sigunguName(),
        row.emdName(),
        row.facilityAddress(),
        row.latitude(),
        row.longitude(),
        row.phoneNumber(),
        row.homepageUrl());
  }

  private Program createProgram(
      Facility facility, DataImport dataImport, String programKey, ProgramCsvRow row) {
    return Program.create(
        facility,
        dataImport,
        programKey,
        row.typeName(),
        row.name(),
        row.targetName(),
        row.startsOn(),
        row.endsOn(),
        row.weekdayText(),
        row.weekdayMask(),
        row.timeText(),
        row.recruitmentCapacity(),
        row.price(),
        row.priceTypeName(),
        row.programCategory(),
        row.adultEligibility(),
        "NORMALIZED");
  }

  private void updateProgram(Program program, DataImport dataImport, ProgramCsvRow row) {
    program.update(
        dataImport,
        row.typeName(),
        row.weekdayText(),
        row.weekdayMask(),
        row.timeText(),
        row.recruitmentCapacity(),
        row.price(),
        row.priceTypeName(),
        row.programCategory(),
        row.adultEligibility(),
        "NORMALIZED");
  }

  private Reader reader(Path csvFile) throws IOException {
    return new InputStreamReader(
        Files.newInputStream(csvFile),
        StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT));
  }

  private String checksum(Path csvFile) {
    try (InputStream inputStream = Files.newInputStream(csvFile)) {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      inputStream.transferTo(
          new java.security.DigestOutputStream(java.io.OutputStream.nullOutputStream(), digest));
      return HexFormat.of().formatHex(digest.digest());
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private String dataVersion(String fileName) {
    int extensionIndex = fileName.lastIndexOf('.');
    return extensionIndex < 0 ? fileName : fileName.substring(0, extensionIndex);
  }

  private String key(String... values) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      for (String value : values) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static final class ImportFailure extends RuntimeException {

    private final int totalCount;
    private final int successCount;
    private final int failureCount;

    private ImportFailure(int totalCount, int successCount, int failureCount, Exception cause) {
      super(cause);
      this.totalCount = totalCount;
      this.successCount = successCount;
      this.failureCount = failureCount;
    }
  }
}
