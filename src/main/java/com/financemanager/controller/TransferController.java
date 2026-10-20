package com.financemanager.controller;

import com.financemanager.entity.Account;
import com.financemanager.entity.User;
import com.financemanager.repository.AccountRepository;
import com.financemanager.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Controller
public class TransferController {
    private final AccountRepository accounts;
    private final UserService users;
    public TransferController(AccountRepository accounts, UserService users) { this.accounts=accounts; this.users=users; }

    @PostMapping("/transfers")
    @Transactional
    public String transfer(@AuthenticationPrincipal UserDetails principal, @RequestParam Long fromAccountId,
                           @RequestParam Long toAccountId, @RequestParam BigDecimal amount, RedirectAttributes flash) {
        User user=users.findByUsername(principal.getUsername());
        if (amount==null || amount.signum()<=0 || fromAccountId.equals(toAccountId)) {
            flash.addFlashAttribute("transferError","Choose two different accounts and enter an amount greater than zero.");
            return "redirect:/dashboard#accounts";
        }
        Account from=accounts.findByIdAndUserId(fromAccountId,user.getId()).orElse(null);
        Account to=accounts.findByIdAndUserId(toAccountId,user.getId()).orElse(null);
        if(from==null || to==null || !from.getCurrency().equalsIgnoreCase(to.getCurrency())) {
            flash.addFlashAttribute("transferError","Accounts must belong to you and use the same currency.");
            return "redirect:/dashboard#accounts";
        }
        if(from.getAccountType()!=Account.Type.CREDIT_CARD && from.getBalance().compareTo(amount)<0) {
            flash.addFlashAttribute("transferError","The source account does not have enough available balance.");
            return "redirect:/dashboard#accounts";
        }
        from.setBalance(from.getBalance().subtract(amount)); to.setBalance(to.getBalance().add(amount));
        accounts.save(from); accounts.save(to);
        flash.addFlashAttribute("transferSuccess","Transfer recorded between your accounts.");
        return "redirect:/dashboard#accounts";
    }
}
