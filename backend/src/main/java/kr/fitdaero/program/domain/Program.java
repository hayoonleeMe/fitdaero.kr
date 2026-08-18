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
}
