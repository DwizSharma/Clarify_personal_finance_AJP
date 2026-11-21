package com.financemanager.controller;

import com.financemanager.entity.Category;
import com.financemanager.entity.User;
import com.financemanager.repository.CategoryRepository;
import com.financemanager.repository.AccountRepository;
import com.financemanager.repository.ExpenseRepository;
import com.financemanager.repository.SavingsGoalRepository;
import com.financemanager.entity.SavingsGoal;
import com.financemanager.entity.Account;
import com.financemanager.entity.PaymentMethod;
import com.financemanager.service.FinanceService;
import com.financemanager.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ExpenseController {

    private final FinanceService financeService;
    private final UserService userService;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final ExpenseRepository expenseRepository;
    private final SavingsGoalRepository savingsGoals;

    @Autowired
    public ExpenseController(FinanceService financeService, UserService userService,
                              CategoryRepository categoryRepository, AccountRepository accountRepository, ExpenseRepository expenseRepository, SavingsGoalRepository savingsGoals) {
        this.financeService = financeService;
        this.userService = userService;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.expenseRepository = expenseRepository;
        this.savingsGoals = savingsGoals;
    }

    @GetMapping("/addExpense")
    public String showForm(@AuthenticationPrincipal UserDetails principal, Model model) {
        model.addAttribute("categories", categoryRepository.findByType(Category.Type.EXPENSE));
        model.addAttribute("accounts", accountRepository.findByUserId(userService.findByUsername(principal.getUsername()).getId()));
        return "add-expense"; // templates/add-expense.html
    }

    @PostMapping("/addExpense")
    @Transactional
    public String addExpense(@AuthenticationPrincipal UserDetails principal,
                              @RequestParam BigDecimal amount,
                              @RequestParam String description,
                              @RequestParam LocalDate date,
                              @RequestParam Long categoryId,
                              @RequestParam(name = "splitCategoryId", required = false) List<Long> splitCategoryIds,
                              @RequestParam(name = "splitAmount", required = false) List<BigDecimal> splitAmounts,
                              @RequestParam(name = "splitNote", required = false) List<String> splitNotes,
                              @RequestParam(required = false) Long accountId,
                              @RequestParam(required = false) PaymentMethod paymentMethod,
                              Model model) {
        User user = userService.findByUsername(principal.getUsername());
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category"));

        try {
            List<FinanceService.SplitInput> splits = resolveSplits(splitCategoryIds, splitAmounts, splitNotes);
            Account account = accountId == null ? null : accountRepository.findByIdAndUserId(accountId, user.getId()).orElseThrow(() -> new IllegalArgumentException("Invalid account"));
            var saved = financeService.addExpense(user, category, amount, description, date, splits);
            if (account != null) { saved.setAccount(account); account.setBalance(account.getBalance().subtract(amount)); accountRepository.save(account); }
            if (paymentMethod != null) saved.setPaymentMethod(paymentMethod);
            expenseRepository.save(saved);
            allocateRoundUp(user, amount);
        } catch (Exception ex) {
            model.addAttribute("error", "Could not save expense: " + ex.getMessage());
            model.addAttribute("categories", categoryRepository.findByType(Category.Type.EXPENSE));
            model.addAttribute("accounts", accountRepository.findByUserId(user.getId()));
            return "add-expense";
        }

        String budgetWarning = financeService.checkBudgetStatus(user.getId(), categoryId, date);
        financeService.checkExpenseAnomaly(user.getId(), categoryId, category.getName(), amount, date);
        model.addAttribute("budgetWarning", budgetWarning);

        return "redirect:/dashboard";
    }

    private void allocateRoundUp(User user, BigDecimal amount) {
        BigDecimal hundred = BigDecimal.valueOf(100);
        BigDecimal roundUp = amount.divide(hundred, 0, java.math.RoundingMode.CEILING)
                .multiply(hundred).subtract(amount).setScale(2, java.math.RoundingMode.HALF_UP);
        if (roundUp.signum() <= 0) return;
        SavingsGoal bucket = savingsGoals.findByUserId(user.getId()).stream()
                .filter(g -> g.getGoalName().equalsIgnoreCase("Emergency Fund") || g.getGoalName().equalsIgnoreCase("Gadget Goal"))
                .findFirst().orElseGet(() -> {
                    SavingsGoal goal = new SavingsGoal();
                    goal.setGoalName("Emergency Fund");
                    goal.setTargetAmount(new BigDecimal("25000.00"));
                    goal.setCurrentAmount(BigDecimal.ZERO);
                    goal.setLocked(true);
                    goal.setUser(user);
                    return savingsGoals.save(goal);
                });
        bucket.setLocked(true);
        bucket.setCurrentAmount(bucket.getCurrentAmount().add(roundUp));
        savingsGoals.save(bucket);
    }

    /** Turns the parallel splitCategoryId[]/splitAmount[]/splitNote[] form arrays into SplitInputs. */
    private List<FinanceService.SplitInput> resolveSplits(List<Long> categoryIds, List<BigDecimal> amounts, List<String> notes) {
        if (categoryIds == null || amounts == null || categoryIds.size() < 2 || categoryIds.size() != amounts.size()) {
            return null;
        }
        List<FinanceService.SplitInput> splits = new ArrayList<>();
        for (int i = 0; i < categoryIds.size(); i++) {
            Long catId = categoryIds.get(i);
            BigDecimal amt = amounts.get(i);
            if (catId == null || amt == null) continue;
            Category splitCategory = categoryRepository.findById(catId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid split category"));
            String note = (notes != null && notes.size() > i) ? notes.get(i) : null;
            splits.add(new FinanceService.SplitInput(catId, splitCategory, amt, note));
        }
        return splits.size() >= 2 ? splits : null;
    }
}
