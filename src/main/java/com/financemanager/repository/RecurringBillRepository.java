package com.financemanager.repository;

import com.financemanager.entity.RecurringBill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface RecurringBillRepository extends JpaRepository<RecurringBill,Long> {
    List<RecurringBill> findByUserIdAndActiveTrueAndNextDueDateBetweenOrderByNextDueDateAsc(Long userId, LocalDate start, LocalDate end);
}
