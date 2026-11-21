package com.financemanager.service;

import com.financemanager.entity.Notification;
import com.financemanager.entity.WishVaultItem;
import com.financemanager.repository.NotificationRepository;
import com.financemanager.repository.WishVaultRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class CoolingOffScheduler {
    private final WishVaultRepository wishes;
    private final NotificationRepository notifications;

    public CoolingOffScheduler(WishVaultRepository wishes, NotificationRepository notifications) {
        this.wishes = wishes; this.notifications = notifications;
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void releaseExpiredWishesForReview() {
        for (WishVaultItem item : wishes.findExpired(WishVaultItem.Status.WAITING, LocalDateTime.now())) {
            item.setStatus(WishVaultItem.Status.READY);
            notifications.save(new Notification("Your 72-hour wait for “" + item.getItemName() + "” has ended. Choose Keep it or save ₹" + item.getAmount() + " to your Emergency Fund.", item.getUser()));
        }
    }
}
