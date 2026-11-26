package com.financemanager.repository;

import com.financemanager.entity.RecurringBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface RecurringBillRepository extends JpaRepository<RecurringBill, Long> {

    // JOIN FETCH user so the association is initialised before the session closes.
    @Query("SELECT b FROM RecurringBill b JOIN FETCH b.user " +
           "WHERE b.user.id = :userId AND b.active = true " +
           "AND b.nextDueDate BETWEEN :start AND :end ORDER BY b.nextDueDate ASC")
    List<RecurringBill> findByUserIdAndActiveTrueAndNextDueDateBetweenOrderByNextDueDateAsc(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("SELECT b FROM RecurringBill b JOIN FETCH b.user WHERE b.active = true AND b.nextDueDate = :date")
    List<RecurringBill> findActiveDueOn(@Param("date") LocalDate date);
}
