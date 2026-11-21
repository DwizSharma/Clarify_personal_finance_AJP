package com.financemanager.service;

import com.financemanager.entity.Notification;
import com.financemanager.repository.ExpenseRepository;
import com.financemanager.repository.NotificationRepository;
import com.financemanager.repository.RecurringBillRepository;
import com.financemanager.repository.UserRepository;
import com.financemanager.entity.User;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class RecurringLeakDetector {
    private final ExpenseRepository expenses;
    private final RecurringBillRepository bills;
    private final NotificationRepository notifications;
    private final UserRepository users;

    public RecurringLeakDetector(ExpenseRepository expenses, RecurringBillRepository bills,
                                 NotificationRepository notifications, UserRepository users) {
        this.expenses = expenses;
        this.bills = bills;
        this.notifications = notifications;
        this.users = users;
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void notifyUpcomingRenewals() {
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(3);
        bills.findActiveDueOn(dueDate).forEach(bill -> notifications.save(new Notification(
                "Renewal reminder: " + bill.getBillName() + " for " + bill.getAmount() + " is due in 3 days.", bill.getUser())));

        Map<RecurringKey, List<LocalDate>> occurrences = new HashMap<>();
        for (Object[] row : expenses.findRecurringScanRows()) {
            Long userId = (Long) row[0];
            String description = ((String) row[1]).trim();
            BigDecimal amount = ((BigDecimal) row[2]).setScale(2, BigDecimal.ROUND_HALF_UP);
            LocalDate date = (LocalDate) row[3];
            if (description.isEmpty()) continue;
            occurrences.computeIfAbsent(new RecurringKey(userId, description, amount), ignored -> new ArrayList<>()).add(date);
        }
        occurrences.forEach((key, dates) -> {
            dates.sort(Comparator.naturalOrder());
            for (int interval : List.of(7, 30, 365)) {
                long matchingGaps = 0;
                for (int i = 1; i < dates.size(); i++) {
                    long gap = ChronoUnit.DAYS.between(dates.get(i - 1), dates.get(i));
                    long tolerance = interval == 7 ? 1 : interval == 30 ? 2 : 3;
                    if (Math.abs(gap - interval) <= tolerance) matchingGaps++;
                }
                if (matchingGaps >= 2 && dates.get(dates.size() - 1).plusDays(interval).equals(dueDate)) {
                    notifications.findByUserIdOrderByCreatedAtDesc(key.userId()).stream()
                            .filter(n -> n.getMessage().contains(key.description()) && n.getMessage().contains(dueDate.toString()))
                            .findFirst().orElseGet(() -> {
                                User user = users.findById(key.userId()).orElse(null);
                                if (user == null) return null;
                                return notifications.save(new Notification(
                                        "Possible recurring charge: " + key.description() + " (₹" + key.amount().toPlainString() + ") may renew in 3 days on " + dueDate + ". Review your transactions.", user));
                            });
                    break;
                }
            }
        });
    }

    private record RecurringKey(Long userId, String description, BigDecimal amount) {}
}
