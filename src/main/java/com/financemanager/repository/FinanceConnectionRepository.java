package com.financemanager.repository;

import com.financemanager.entity.FinanceConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FinanceConnectionRepository extends JpaRepository<FinanceConnection, Long> {
    List<FinanceConnection> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<FinanceConnection> findByIdAndUserId(Long id, Long userId);
}
