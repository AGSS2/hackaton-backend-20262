package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.entity.Playthrough;
import com.tuckersoft.branchengine.entity.user;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaythroughRepository extends JpaRepository<Playthrough, Long> {
    Optional<Playthrough> findByPlayerTag(String playerTag);
    boolean existsByPlayerTag(String playerTag);
    List<Playthrough> findByUserOrderByCreatedAtDesc(user user);
    List<Playthrough> findAllByOrderByCreatedAtDesc();
}