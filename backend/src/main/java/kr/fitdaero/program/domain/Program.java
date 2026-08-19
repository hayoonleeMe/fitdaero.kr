package kr.fitdaero.program.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import kr.fitdaero.dataimport.domain.DataImport;
import kr.fitdaero.facility.domain.Facility;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
public class Program {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private Facility facility;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "import_id", nullable = false)
  private DataImport dataImport;

  @Column(nullable = false, length = 64)
  private String sourceKey;

  private String typeName;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String targetName;

  @Column(nullable = false)
  private LocalDate startsOn;

  @Column(nullable = false)
  private LocalDate endsOn;

  @Column(nullable = false)
  private String weekdayText;

  private Byte weekdayMask;

  @Column(nullable = false)
  private String timeText;

  private Integer recruitmentCapacity;

  @Getter
  @Column(precision = 12, scale = 2)
  private BigDecimal price;

  @Column(length = 100)
  private String priceTypeName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private ProgramCategory programCategory;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private AdultEligibility adultEligibility;

  @Column(nullable = false, length = 30)
  private String normalizationStatus;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  protected Program() {}

  public static Program create(
      Facility facility,
      DataImport dataImport,
      String sourceKey,
      String name,
      String targetName,
      LocalDate startsOn,
      LocalDate endsOn,
      String weekdayText,
      String timeText,
      ProgramCategory programCategory,
      AdultEligibility adultEligibility,
      String normalizationStatus) {
    Program program = new Program();
    program.facility = facility;
    program.dataImport = dataImport;
    program.sourceKey = sourceKey;
    program.name = name;
    program.targetName = targetName;
    program.startsOn = startsOn;
    program.endsOn = endsOn;
    program.weekdayText = weekdayText;
    program.timeText = timeText;
    program.programCategory = programCategory;
    program.adultEligibility = adultEligibility;
    program.normalizationStatus = normalizationStatus;
    return program;
  }

  public static Program create(
      Facility facility,
      DataImport dataImport,
      String sourceKey,
      String typeName,
      String name,
      String targetName,
      LocalDate startsOn,
      LocalDate endsOn,
      String weekdayText,
      Byte weekdayMask,
      String timeText,
      Integer recruitmentCapacity,
      BigDecimal price,
      String priceTypeName,
      ProgramCategory programCategory,
      AdultEligibility adultEligibility,
      String normalizationStatus) {
    Program program =
        create(
            facility,
            dataImport,
            sourceKey,
            name,
            targetName,
            startsOn,
            endsOn,
            weekdayText,
            timeText,
            programCategory,
            adultEligibility,
            normalizationStatus);
    program.update(
        dataImport,
        typeName,
        weekdayText,
        weekdayMask,
        timeText,
        recruitmentCapacity,
        price,
        priceTypeName,
        programCategory,
        adultEligibility,
        normalizationStatus);
    return program;
  }

  public void update(
      DataImport dataImport,
      String typeName,
      String weekdayText,
      Byte weekdayMask,
      String timeText,
      Integer recruitmentCapacity,
      BigDecimal price,
      String priceTypeName,
      ProgramCategory programCategory,
      AdultEligibility adultEligibility,
      String normalizationStatus) {
    this.dataImport = dataImport;
    this.typeName = typeName;
    this.weekdayText = weekdayText;
    this.weekdayMask = weekdayMask;
    this.timeText = timeText;
    this.recruitmentCapacity = recruitmentCapacity;
    this.price = price;
    this.priceTypeName = priceTypeName;
    this.programCategory = programCategory;
    this.adultEligibility = adultEligibility;
    this.normalizationStatus = normalizationStatus;
  }
}
