package com.financemanager.repository;

import com.financemanager.entity.RecurringBill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecurringBillRepository extends JpaRepository<RecurringBill,Long> {
    List<RecurringBill> findByUserIdAndActiveTrueAndNextDueDateBetweenOrderByNextDueDateAsc(Long userId, LocalDate start, LocalDate end);

    @Query("SELECT b FROM RecurringBill b JOIN FETCH b.user WHERE b.active = true AND b.nextDueDate = :date")
    List<RecurringBill> findActiveDueOn(@Param("date") LocalDate date);
}
