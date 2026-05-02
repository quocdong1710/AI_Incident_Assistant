package com.ptit.aia.repository;

import com.ptit.aia.domain.Engineer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EngineerRepository extends JpaRepository<Engineer, Long> {
    Optional<Engineer> findByUsername(String username);
}
