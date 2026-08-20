package kr.fitdaero.program.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, Long> {

  List<Program> findBySourceKeyIn(Collection<String> sourceKeys);
}
