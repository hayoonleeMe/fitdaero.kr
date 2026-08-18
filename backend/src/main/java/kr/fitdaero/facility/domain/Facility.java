package kr.fitdaero.facility.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
public class Facility {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 64)
  private String sourceKey;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, length = 20)
  private String sidoCode;

  @Column(nullable = false, length = 100)
  private String sidoName;

  @Column(nullable = false, length = 20)
  private String sigunguCode;

  @Column(nullable = false, length = 100)
  private String sigunguName;

  @Column(length = 100)
  private String emdName;

  @Column(nullable = false, length = 500)
  private String address;

  @Column(precision = 10, scale = 7)
  private BigDecimal latitude;

  @Column(precision = 10, scale = 7)
  private BigDecimal longitude;

  @Column(length = 30)
  private String phoneNumber;

  @Column(length = 2048)
  private String homepageUrl;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  protected Facility() {}
}
