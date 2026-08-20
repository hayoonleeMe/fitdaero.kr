package kr.fitdaero.facility.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
public class Facility {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Getter
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

  public static Facility create(
      String sourceKey,
      String name,
      String sidoCode,
      String sidoName,
      String sigunguCode,
      String sigunguName,
      String address) {
    Facility facility = new Facility();
    facility.sourceKey = sourceKey;
    facility.name = name;
    facility.sidoCode = sidoCode;
    facility.sidoName = sidoName;
    facility.sigunguCode = sigunguCode;
    facility.sigunguName = sigunguName;
    facility.address = address;
    return facility;
  }

  public static Facility create(
      String sourceKey,
      String name,
      String sidoCode,
      String sidoName,
      String sigunguCode,
      String sigunguName,
      String emdName,
      String address,
      BigDecimal latitude,
      BigDecimal longitude,
      String phoneNumber,
      String homepageUrl) {
    Facility facility =
        create(sourceKey, name, sidoCode, sidoName, sigunguCode, sigunguName, address);
    facility.update(
        name,
        sidoCode,
        sidoName,
        sigunguCode,
        sigunguName,
        emdName,
        address,
        latitude,
        longitude,
        phoneNumber,
        homepageUrl);
    return facility;
  }

  public void update(
      String name,
      String sidoCode,
      String sidoName,
      String sigunguCode,
      String sigunguName,
      String emdName,
      String address,
      BigDecimal latitude,
      BigDecimal longitude,
      String phoneNumber,
      String homepageUrl) {
    this.name = name;
    this.sidoCode = sidoCode;
    this.sidoName = sidoName;
    this.sigunguCode = sigunguCode;
    this.sigunguName = sigunguName;
    this.emdName = emdName;
    this.address = address;
    this.latitude = latitude;
    this.longitude = longitude;
    this.phoneNumber = phoneNumber;
    this.homepageUrl = homepageUrl;
  }
}
