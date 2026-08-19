package kr.fitdaero.facility.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityRepository extends JpaRepository<Facility, Long> {

  Optional<Facility> findBySourceKey(String sourceKey);
}
