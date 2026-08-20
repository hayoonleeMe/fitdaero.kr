package kr.fitdaero.program.importer;

import java.math.BigDecimal;
import java.time.LocalDate;
import kr.fitdaero.program.domain.AdultEligibility;
import kr.fitdaero.program.domain.ProgramCategory;

record ProgramCsvRow(
    String facilityName,
    String facilityAddress,
    String sidoCode,
    String sidoName,
    String sigunguCode,
    String sigunguName,
    String emdName,
    BigDecimal latitude,
    BigDecimal longitude,
    String phoneNumber,
    String homepageUrl,
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
    AdultEligibility adultEligibility) {}
