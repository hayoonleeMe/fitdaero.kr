package kr.fitdaero.program.importer;

import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class ProgramCsvImportStartupRunner implements ApplicationRunner {

  private final ProgramCsvImporter importer;
  private final boolean importOnStartup;
  private final String csvPath;

  ProgramCsvImportStartupRunner(
      ProgramCsvImporter importer,
      @Value("${app.import.on-startup}") boolean importOnStartup,
      @Value("${app.import.program-csv-path}") String csvPath) {
    this.importer = importer;
    this.importOnStartup = importOnStartup;
    this.csvPath = csvPath;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!importOnStartup) {
      log.info("Program CSV import is disabled");
      return;
    }

    if (csvPath.isBlank()) {
      log.warn("Program CSV import is skipped because APP_PROGRAM_CSV_PATH is empty");
      return;
    }

    try {
      Path path = Path.of(csvPath);
      if (!Files.isRegularFile(path)) {
        log.warn("Program CSV import is skipped because the path is not a file: {}", path);
        return;
      }
      log.info("Program CSV import started: {}", path);
      switch (importer.importFile(path)) {
        case COMPLETED -> log.info("Program CSV import completed: {}", path);
        case SKIPPED ->
            log.info("Program CSV import skipped because it was already processed: {}", path);
        case FAILED -> log.warn("Program CSV import failed: {}", path);
      }
    } catch (RuntimeException exception) {
      log.warn("Program CSV import failed at startup", exception);
    }
  }
}
