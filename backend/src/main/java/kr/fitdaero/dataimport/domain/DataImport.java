package kr.fitdaero.dataimport.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
public class DataImport {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private DataImportSourceType sourceType;

  @Column(nullable = false)
  private String dataVersion;

  private String fileName;

  @Column(length = 64)
  private String fileChecksum;

  @Column(length = 1024)
  private String sourceLocator;

  @Column(length = 64)
  private String requestSignature;

  @Column(length = 6)
  private String collectedFromYm;

  @Column(length = 6)
  private String collectedToYm;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DataImportStatus status;

  @Column(nullable = false)
  private int totalCount;

  @Column(nullable = false)
  private int successCount;

  @Column(nullable = false)
  private int failureCount;

  @Column(length = 1000)
  private String lastErrorMessage;

  @CreationTimestamp
  @Column(nullable = false)
  private LocalDateTime startedAt;

  private LocalDateTime completedAt;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  protected DataImport() {}

  public static DataImport startPublicFacilityProgram(
      String dataVersion, String fileName, String fileChecksum) {
    DataImport dataImport = new DataImport();
    dataImport.sourceType = DataImportSourceType.PUBLIC_FACILITY_PROGRAM;
    dataImport.dataVersion = dataVersion;
    dataImport.fileName = fileName;
    dataImport.fileChecksum = fileChecksum;
    dataImport.status = DataImportStatus.RUNNING;
    return dataImport;
  }
}
