package kr.fitdaero.facility.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityRepository extends JpaRepository<Facility, Long> {

  List<Facility> findBySourceKeyIn(Collection<String> sourceKeys);
}
