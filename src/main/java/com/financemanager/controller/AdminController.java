package com.financemanager.controller;

import com.financemanager.entity.User;
import com.financemanager.repository.AccountRepository;
import com.financemanager.repository.BudgetRepository;
import com.financemanager.repository.ExpenseRepository;
import com.financemanager.repository.IncomeRepository;
import com.financemanager.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository users;
    private final ExpenseRepository expenses;
    private final IncomeRepository incomes;
    private final BudgetRepository budgets;
    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository users, ExpenseRepository expenses,
                           IncomeRepository incomes, BudgetRepository budgets,
                           AccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.expenses = expenses;
        this.incomes = incomes;
        this.budgets = budgets;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String dashboard(Model model) {
        List<User> allUsers = users.findAll();
        long totalUsers  = allUsers.size();
        long adminCount  = allUsers.stream().filter(u -> u.getRole() == User.Role.ROLE_ADMIN).count();
        long regularCount = totalUsers - adminCount;

        LocalDate start = LocalDate.of(1970, 1, 1);
        LocalDate end   = LocalDate.now();

        // Platform-wide totals (sum across all users)
        BigDecimal totalExpenses = BigDecimal.ZERO;
        BigDecimal totalIncomeAmt = BigDecimal.ZERO;
        for (User u : allUsers) {
            totalExpenses  = totalExpenses.add(expenses.sumByUserAndDateRange(u.getId(), start, end));
            totalIncomeAmt = totalIncomeAmt.add(incomes.sumByUserAndDateRange(u.getId(), start, end));
        }

        // Per-user summary rows
        List<Map<String, Object>> userRows = new ArrayList<>();
        for (User u : allUsers) {
            BigDecimal spent  = expenses.sumByUserAndDateRange(u.getId(), start, end);
            BigDecimal earned = incomes.sumByUserAndDateRange(u.getId(), start, end);
            int accountCount  = accounts.findByUserId(u.getId()).size();
            int budgetCount   = budgets.findByUserId(u.getId()).size();
            userRows.add(Map.of(
                "id",           u.getId(),
                "username",     u.getUsername(),
                "email",        u.getEmail(),
                "role",         u.getRole().name(),
                "createdAt",    u.getCreatedAt().toLocalDate(),
                "totalSpent",   spent,
                "totalEarned",  earned,
                "accounts",     accountCount,
                "budgets",      budgetCount
            ));
        }
        userRows.sort((a, b) -> ((Long) a.get("id")).compareTo((Long) b.get("id")));

        model.addAttribute("totalUsers",    totalUsers);
        model.addAttribute("adminCount",    adminCount);
        model.addAttribute("regularCount",  regularCount);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("totalIncome",   totalIncomeAmt);
        model.addAttribute("userRows",      userRows);
        return "admin/dashboard";
    }

    /** Promote a user to ROLE_ADMIN. */
    @PostMapping("/users/{id}/promote")
    public String promote(@PathVariable Long id, RedirectAttributes ra) {
        users.findById(id).ifPresent(u -> {
            u.setRole(User.Role.ROLE_ADMIN);
            users.save(u);
        });
        ra.addFlashAttribute("success", "User promoted to admin.");
        return "redirect:/admin";
    }

    /** Demote an admin back to ROLE_USER. */
    @PostMapping("/users/{id}/demote")
    public String demote(@PathVariable Long id, RedirectAttributes ra) {
        users.findById(id).ifPresent(u -> {
            u.setRole(User.Role.ROLE_USER);
            users.save(u);
        });
        ra.addFlashAttribute("success", "User demoted to regular user.");
        return "redirect:/admin";
    }

    /** Delete a user and all their data (cascades via JPA). */
    @PostMapping("/users/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        users.deleteById(id);
        ra.addFlashAttribute("success", "User deleted.");
        return "redirect:/admin";
    }

    /** Reset a user's password from the admin panel. */
    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam String newPassword,
                                RedirectAttributes ra) {
        if (newPassword == null || newPassword.length() < 6) {
            ra.addFlashAttribute("error", "Password must be at least 6 characters.");
            return "redirect:/admin";
        }
        users.findById(id).ifPresent(u -> {
            u.setPasswordHash(passwordEncoder.encode(newPassword));
            users.save(u);
        });
        ra.addFlashAttribute("success", "Password reset successfully.");
        return "redirect:/admin";
    }
}
