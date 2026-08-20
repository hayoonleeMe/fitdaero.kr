package kr.fitdaero.program.importer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProgramCsvImportStartupRunnerTest {

  @TempDir Path tempDir;

  @Test
  void doesNotAccessThePathWhenImportIsDisabled() {
    ProgramCsvImporter importer = mock(ProgramCsvImporter.class);

    new ProgramCsvImportStartupRunner(importer, false, tempDir.resolve("missing.csv").toString())
        .run(null);

    verifyNoInteractions(importer);
  }

  @Test
  void skipsAnEmptyOrMissingPath() {
    ProgramCsvImporter importer = mock(ProgramCsvImporter.class);

    new ProgramCsvImportStartupRunner(importer, true, "").run(null);
    new ProgramCsvImportStartupRunner(importer, true, tempDir.resolve("missing.csv").toString())
        .run(null);

    verifyNoInteractions(importer);
  }

  @Test
  void importsARegularFile() throws IOException {
    ProgramCsvImporter importer = mock(ProgramCsvImporter.class);
    Path csvFile = Files.createFile(tempDir.resolve("program.csv"));
    when(importer.importFile(csvFile)).thenReturn(ProgramCsvImporter.ImportResult.COMPLETED);

    new ProgramCsvImportStartupRunner(importer, true, csvFile.toString()).run(null);

    verify(importer).importFile(csvFile);
  }

  @Test
  void keepsStartingWhenImportFails() throws IOException {
    ProgramCsvImporter importer = mock(ProgramCsvImporter.class);
    Path csvFile = Files.createFile(tempDir.resolve("program.csv"));
    when(importer.importFile(csvFile)).thenReturn(ProgramCsvImporter.ImportResult.FAILED);

    assertThatCode(
            () -> new ProgramCsvImportStartupRunner(importer, true, csvFile.toString()).run(null))
        .doesNotThrowAnyException();
  }

  @Test
  void keepsStartingWhenImporterThrows() throws IOException {
    ProgramCsvImporter importer = mock(ProgramCsvImporter.class);
    Path csvFile = Files.createFile(tempDir.resolve("program.csv"));
    doThrow(new IllegalStateException()).when(importer).importFile(csvFile);

    assertThatCode(
            () -> new ProgramCsvImportStartupRunner(importer, true, csvFile.toString()).run(null))
        .doesNotThrowAnyException();
  }
}
