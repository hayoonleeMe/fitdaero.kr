package kr.fitdaero.program.importer;

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
    String typeName,
    String name,
    String targetName,
    LocalDate startsOn,
    LocalDate endsOn,
    String weekdayText,
    Byte weekdayMask,
    String timeText,
    ProgramCategory programCategory,
    AdultEligibility adultEligibility) {}
