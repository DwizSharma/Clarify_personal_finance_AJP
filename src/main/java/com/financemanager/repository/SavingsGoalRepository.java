package com.financemanager.repository;

import com.financemanager.entity.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    @Query("SELECT g FROM SavingsGoal g JOIN FETCH g.user WHERE g.user.id = :userId")
    List<SavingsGoal> findByUserId(@Param("userId") Long userId);
}
