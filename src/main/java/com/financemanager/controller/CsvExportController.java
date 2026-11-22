package com.financemanager.controller;

import com.financemanager.entity.Expense;
import com.financemanager.entity.Income;
import com.financemanager.entity.User;
import com.financemanager.service.FinanceService;
import com.financemanager.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

@Controller
public class CsvExportController {

    private final FinanceService finance;
    private final UserService users;

    public CsvExportController(FinanceService finance, UserService users) {
        this.finance = finance;
        this.users = users;
    }

    @GetMapping("/transactions/export")
    public void exportCsv(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false, defaultValue = "all") String type,
            HttpServletResponse response) throws IOException {

        User user = users.findByUsername(principal.getUsername());

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"transactions.csv\"");

        PrintWriter writer = response.getWriter();
        writer.println("Date,Type,Category,Description,Amount");

        if (!type.equalsIgnoreCase("income")) {
            List<Expense> expenses = finance.getExpenseHistory(user.getId());
            for (Expense e : expenses) {
                if (from != null && e.getDate().isBefore(from)) continue;
                if (to   != null && e.getDate().isAfter(to))   continue;
                writer.printf("%s,Expense,%s,%s,%.2f%n",
                        e.getDate(),
                        escapeCsv(e.getCategory().getName()),
                        escapeCsv(e.getDescription() != null ? e.getDescription() : ""),
                        e.getAmount());
            }
        }

        if (!type.equalsIgnoreCase("expense")) {
            List<Income> incomes = finance.getIncomeHistory(user.getId());
            for (Income i : incomes) {
                if (from != null && i.getDate().isBefore(from)) continue;
                if (to   != null && i.getDate().isAfter(to))   continue;
                writer.printf("%s,Income,%s,%s,%.2f%n",
                        i.getDate(),
                        escapeCsv(i.getCategory().getName()),
                        escapeCsv(i.getDescription() != null ? i.getDescription() : ""),
                        i.getAmount());
            }
        }

        writer.flush();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
