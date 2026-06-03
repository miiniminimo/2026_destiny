package com.destinycode.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SajuRepository extends JpaRepository<SajuInfo, Long> {
    Optional<SajuInfo> findByUserEmail(String email);
    Optional<SajuInfo> findByUser(User user);
    boolean existsByUserEmail(String email);
}
