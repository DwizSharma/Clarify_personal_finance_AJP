package com.financemanager.repository;

import com.financemanager.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    // Category is LAZY by default; JOIN FETCH it here so callers can access
    // budget.getCategory().getName() without an open Hibernate session.
    @Query("SELECT b FROM Budget b LEFT JOIN FETCH b.category WHERE b.user.id = :userId")
    List<Budget> findByUserId(@Param("userId") Long userId);

    @Query("SELECT b FROM Budget b LEFT JOIN FETCH b.category WHERE b.user.id = :userId AND b.category.id = :categoryId")
    Optional<Budget> findByUserIdAndCategoryId(@Param("userId") Long userId, @Param("categoryId") Long categoryId);
}
