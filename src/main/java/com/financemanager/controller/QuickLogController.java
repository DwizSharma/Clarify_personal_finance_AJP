package com.financemanager.controller;

import com.financemanager.entity.Account;
import com.financemanager.entity.Category;
import com.financemanager.repository.AccountRepository;
import com.financemanager.repository.CategoryRepository;
import com.financemanager.repository.ExpenseRepository;
import com.financemanager.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class QuickLogController {
    private final UserService users;
    private final CategoryRepository categories;
    private final AccountRepository accounts;
    private final ExpenseRepository expenses;

    public QuickLogController(UserService users, CategoryRepository categories,
                              AccountRepository accounts, ExpenseRepository expenses) {
        this.users = users;
        this.categories = categories;
        this.accounts = accounts;
        this.expenses = expenses;
    }

    @GetMapping("/api/quick-log/options")
    public QuickLogOptions options(@AuthenticationPrincipal UserDetails principal) {
        Long userId = users.findByUsername(principal.getUsername()).getId();
        List<Choice> categoryChoices = categories.findByType(Category.Type.EXPENSE).stream()
                .map(c -> new Choice(c.getId(), c.getName())).toList();
        List<Choice> accountChoices = accounts.findByUserId(userId).stream()
                .map(a -> new Choice(a.getId(), (a.getMemberName() == null ? "Me" : a.getMemberName()) + " · " + a.getAccountName())).toList();
        List<String> merchants = expenses.findRecentDescriptions(userId, PageRequest.of(0, 12));
        return new QuickLogOptions(categoryChoices, accountChoices, merchants);
    }

    public record Choice(Long id, String name) {}
    public record QuickLogOptions(List<Choice> categories, List<Choice> accounts, List<String> merchants) {}
}
