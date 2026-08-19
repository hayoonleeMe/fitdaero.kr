package kr.fitdaero.program.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, Long> {

  Optional<Program> findBySourceKey(String sourceKey);
}
