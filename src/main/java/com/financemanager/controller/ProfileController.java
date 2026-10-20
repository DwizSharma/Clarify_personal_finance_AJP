package com.financemanager.controller;

import com.financemanager.entity.FinanceConnection;
import com.financemanager.entity.User;
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

    public ProfileController(UserService users, UserRepository userRepository, FinanceConnectionRepository connections) {
        this.users = users;
        this.userRepository = userRepository;
        this.connections = connections;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = users.findByUsername(principal.getUsername());
        model.addAttribute("user", user);
        var userConnections = connections.findByUserIdOrderByCreatedAtDesc(user.getId());
        model.addAttribute("connections", userConnections);
        model.addAttribute("banks", userConnections.stream()
                .filter(c -> c.getType() == FinanceConnection.Type.BANK).toList());
        model.addAttribute("paymentApps", userConnections.stream()
                .filter(c -> c.getType() == FinanceConnection.Type.PAYMENT_APP).toList());
        return "profile";
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
}
