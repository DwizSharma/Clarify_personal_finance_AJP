package com.financemanager.controller;

import com.financemanager.entity.FinanceConnection;
import com.financemanager.entity.User;
import com.financemanager.entity.Account;
import com.financemanager.repository.AccountRepository;
import com.financemanager.repository.FinanceConnectionRepository;
import com.financemanager.repository.UserRepository;
import com.financemanager.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {
    private final UserService users;
    private final UserRepository userRepository;
    private final FinanceConnectionRepository connections;
    private final AccountRepository accounts;

    public ProfileController(UserService users, UserRepository userRepository, FinanceConnectionRepository connections, AccountRepository accounts) {
        this.users = users;
        this.userRepository = userRepository;
        this.connections = connections;
        this.accounts = accounts;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = users.findByUsername(principal.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("trackedAccounts", accounts.findByUserIdOrderByMemberNameAscAccountNameAsc(user.getId()));
        var userConnections = connections.findByUserIdOrderByCreatedAtDesc(user.getId());
        model.addAttribute("connections", userConnections);
        model.addAttribute("banks", userConnections.stream()
                .filter(c -> c.getType() == FinanceConnection.Type.BANK).toList());
        model.addAttribute("paymentApps", userConnections.stream()
                .filter(c -> c.getType() == FinanceConnection.Type.PAYMENT_APP).toList());
        return "profile";
    }

    @PostMapping("/profile/accounts")
    public String addAccount(@AuthenticationPrincipal UserDetails principal,
                             @RequestParam String institutionName,
                             @RequestParam String accountName,
                             @RequestParam Account.Type accountType,
                             @RequestParam(required = false) String memberName,
                             @RequestParam java.math.BigDecimal balance,
                             @RequestParam(defaultValue = "INR") String currency,
                             RedirectAttributes redirect) {
        String bank = institutionName.trim();
        String label = accountName.trim();
        String member = memberName == null || memberName.isBlank() ? "Me" : memberName.trim();
        String currencyCode = currency.trim().toUpperCase(java.util.Locale.ROOT);
        if (bank.isBlank() || label.isBlank() || balance.signum() < 0 || !currencyCode.matches("[A-Z]{3}")) {
            redirect.addFlashAttribute("profileError", "Enter a bank, account label, non-negative balance and a 3-letter currency code.");
            return "redirect:/profile#tracked-accounts";
        }
        Account account = new Account();
        account.setInstitutionName(limit(bank, 100));
        account.setAccountName(limit(label, 100));
        account.setMemberName(limit(member, 80));
        account.setAccountType(accountType);
        account.setCurrency(currencyCode);
        account.setBalance(accountType == Account.Type.CREDIT_CARD ? balance.negate() : balance);
        account.setUser(users.findByUsername(principal.getUsername()));
        accounts.save(account);
        redirect.addFlashAttribute("profileMessage", "Account added to your household overview.");
        return "redirect:/profile#tracked-accounts";
    }

    @PostMapping("/profile/email")
    public String updateEmail(@AuthenticationPrincipal UserDetails principal, @RequestParam String email,
                              RedirectAttributes redirect) {
        User user = users.findByUsername(principal.getUsername());
        String cleanEmail = email.trim();
        if (!cleanEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            redirect.addFlashAttribute("profileError", "Enter a valid email address.");
            return "redirect:/profile";
        }
        if (userRepository.existsByEmailAndIdNot(cleanEmail, user.getId())) {
            redirect.addFlashAttribute("profileError", "That email is already linked to another account.");
            return "redirect:/profile";
        }
        user.setEmail(cleanEmail);
        userRepository.save(user);
        redirect.addFlashAttribute("profileMessage", "Your profile email has been updated.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/connections")
    public String addConnection(@AuthenticationPrincipal UserDetails principal,
                                @RequestParam FinanceConnection.Type type,
                                @RequestParam String provider,
                                @RequestParam String displayName,
                                @RequestParam(required = false) String identifierHint,
                                @RequestParam(required = false) String note,
                                RedirectAttributes redirect) {
        if (provider.isBlank() || displayName.isBlank()) {
            redirect.addFlashAttribute("profileError", "Add a bank or payment app name and a label.");
            return "redirect:/profile";
        }
        FinanceConnection connection = new FinanceConnection();
        connection.setType(type);
        connection.setProvider(limit(provider.trim(), 80));
        connection.setDisplayName(limit(displayName.trim(), 100));
        connection.setIdentifierHint(blankToNull(identifierHint, 100));
        connection.setNote(blankToNull(note, 180));
        connection.setUser(users.findByUsername(principal.getUsername()));
        connections.save(connection);
        redirect.addFlashAttribute("profileMessage", type == FinanceConnection.Type.BANK
                ? "Bank details saved to your profile." : "Payment app details saved to your profile.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/connections/{id}/delete")
    public String deleteConnection(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal,
                                   RedirectAttributes redirect) {
        User user = users.findByUsername(principal.getUsername());
        var connection = connections.findByIdAndUserId(id, user.getId());
        if (connection.isPresent()) {
            connections.delete(connection.get());
            redirect.addFlashAttribute("profileMessage", "Connection removed from your profile.");
        } else {
            redirect.addFlashAttribute("profileError", "That saved connection could not be found. Refresh your profile and try again.");
        }
        return "redirect:/profile";
    }

    private String limit(String value, int max) { return value.length() > max ? value.substring(0, max) : value; }
    private String blankToNull(String value, int max) {
        if (value == null || value.isBlank()) return null;
        return limit(value.trim(), max);
    }

    @PostMapping("/profile/password")
    public String changePassword(@AuthenticationPrincipal UserDetails principal,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirect) {
        User user = users.findByUsername(principal.getUsername());
        if (!users.checkPassword(user, currentPassword)) {
            redirect.addFlashAttribute("profileError", "Current password is incorrect.");
            return "redirect:/profile";
        }
        if (newPassword.length() < 8) {
            redirect.addFlashAttribute("profileError", "New password must be at least 8 characters.");
            return "redirect:/profile";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirect.addFlashAttribute("profileError", "New passwords do not match.");
            return "redirect:/profile";
        }
        users.changePassword(user, newPassword);
        redirect.addFlashAttribute("profileMessage", "Password updated successfully.");
        return "redirect:/profile";
    }
}
