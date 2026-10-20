package com.financemanager.controller;

import com.financemanager.entity.RecurringBill;
import com.financemanager.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import com.financemanager.repository.RecurringBillRepository;

@Controller
public class RecurringBillController {
    private final UserService users; private final RecurringBillRepository bills;
    public RecurringBillController(UserService users,RecurringBillRepository bills){this.users=users;this.bills=bills;}
    @PostMapping("/bills")
    public String add(@AuthenticationPrincipal UserDetails principal,@RequestParam String billName,@RequestParam BigDecimal amount,
                      @RequestParam LocalDate nextDueDate,@RequestParam RecurringBill.Frequency frequency,RedirectAttributes flash){
        if(billName.isBlank()||amount.signum()<=0){flash.addFlashAttribute("billError","Enter a bill name and an amount greater than zero.");return "redirect:/dashboard#bills";}
        if(nextDueDate.isBefore(LocalDate.now())){flash.addFlashAttribute("billError","Choose today or a future due date.");return "redirect:/dashboard#bills";}
        RecurringBill bill=new RecurringBill();bill.setBillName(billName.trim());bill.setAmount(amount);bill.setNextDueDate(nextDueDate);bill.setFrequency(frequency);bill.setUser(users.findByUsername(principal.getUsername()));bills.save(bill);
        flash.addFlashAttribute("billSuccess","Upcoming bill saved.");return "redirect:/dashboard#bills";
    }
}
