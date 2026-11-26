package com.financemanager.repository;

import com.financemanager.entity.WishVaultItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WishVaultRepository extends JpaRepository<WishVaultItem, Long> {

    @Query("SELECT w FROM WishVaultItem w JOIN FETCH w.user " +
           "WHERE w.user.id = :userId AND w.status IN :statuses ORDER BY w.expiresAt ASC")
    List<WishVaultItem> findByUserIdAndStatusInOrderByExpiresAtAsc(
            @Param("userId") Long userId,
            @Param("statuses") List<WishVaultItem.Status> statuses);

    Optional<WishVaultItem> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT w FROM WishVaultItem w WHERE w.status = :status AND w.expiresAt <= :now")
    List<WishVaultItem> findExpired(@Param("status") WishVaultItem.Status status,
                                    @Param("now") LocalDateTime now);
}
