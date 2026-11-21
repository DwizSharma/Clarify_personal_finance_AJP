package com.financemanager.controller;

import com.financemanager.entity.SavingsGoal;
import com.financemanager.entity.User;
import com.financemanager.entity.WishVaultItem;
import com.financemanager.repository.SavingsGoalRepository;
import com.financemanager.repository.WishVaultRepository;
import com.financemanager.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Controller
public class WishVaultController {
    private final UserService users;
    private final WishVaultRepository wishes;
    private final SavingsGoalRepository goals;

    public WishVaultController(UserService users, WishVaultRepository wishes, SavingsGoalRepository goals) {
        this.users = users; this.wishes = wishes; this.goals = goals;
    }

    @PostMapping("/wishlist")
    public String add(@AuthenticationPrincipal UserDetails principal, @RequestParam String itemName,
                      @RequestParam BigDecimal amount, RedirectAttributes flash) {
        if (itemName.isBlank() || amount.signum() <= 0) {
            flash.addFlashAttribute("wishError", "Enter an item and an amount greater than zero.");
            return "redirect:/dashboard#wishlist";
        }
        WishVaultItem item = new WishVaultItem();
        item.setItemName(itemName.trim()); item.setAmount(amount);
        item.setExpiresAt(LocalDateTime.now().plusHours(72));
        item.setUser(users.findByUsername(principal.getUsername()));
        wishes.save(item);
        flash.addFlashAttribute("wishSuccess", "Saved to your 72-hour cooling-off vault.");
        return "redirect:/dashboard#wishlist";
    }

    @PostMapping("/wishlist/{id}/keep")
    @Transactional
    public String keep(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id, RedirectAttributes flash) {
        User user = users.findByUsername(principal.getUsername());
        WishVaultItem item = wishes.findByIdAndUserId(id, user.getId()).orElseThrow();
        if (item.getStatus() == WishVaultItem.Status.READY) item.setStatus(WishVaultItem.Status.PURCHASED);
        flash.addFlashAttribute("wishSuccess", "Wishlist choice saved.");
        return "redirect:/dashboard#wishlist";
    }

    @PostMapping("/wishlist/{id}/save")
    @Transactional
    public String saveInstead(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id, RedirectAttributes flash) {
        User user = users.findByUsername(principal.getUsername());
        WishVaultItem item = wishes.findByIdAndUserId(id, user.getId()).orElseThrow();
        if (item.getStatus() == WishVaultItem.Status.READY) {
            SavingsGoal bucket = goals.findByUserId(user.getId()).stream()
                    .filter(g -> g.getGoalName().equalsIgnoreCase("Emergency Fund"))
                    .findFirst().orElseGet(() -> {
                        SavingsGoal g = new SavingsGoal(); g.setGoalName("Emergency Fund");
                        g.setTargetAmount(new BigDecimal("25000.00")); g.setCurrentAmount(BigDecimal.ZERO);
                        g.setLocked(true); g.setUser(user); return goals.save(g);
                    });
            bucket.setLocked(true); bucket.setCurrentAmount(bucket.getCurrentAmount().add(item.getAmount())); goals.save(bucket);
            item.setStatus(WishVaultItem.Status.SAVED);
            flash.addFlashAttribute("wishSuccess", "₹" + item.getAmount() + " moved to your locked Emergency Fund as Money Saved.");
        }
        return "redirect:/dashboard#wishlist";
    }
}
