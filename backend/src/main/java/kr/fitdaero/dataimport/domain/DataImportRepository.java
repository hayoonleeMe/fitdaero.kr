package kr.fitdaero.dataimport.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataImportRepository extends JpaRepository<DataImport, Long> {

  Optional<DataImport> findBySourceTypeAndFileChecksum(
      DataImportSourceType sourceType, String fileChecksum);

  Optional<DataImport> findFirstBySourceTypeAndStatusOrderByCompletedAtDescIdDesc(
      DataImportSourceType sourceType, DataImportStatus status);
}
