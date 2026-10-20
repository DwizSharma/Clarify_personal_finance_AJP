package com.financemanager.controller;

import com.financemanager.entity.*;
import com.financemanager.repository.*;
import com.financemanager.service.FinanceService;
import com.financemanager.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Controller
public class StatementImportController {
    private final UserService users; private final FinanceService finance; private final CategoryRepository categories;
    private final AccountRepository accounts; private final IncomeRepository incomes; private final ExpenseRepository expenses;
    public StatementImportController(UserService users, FinanceService finance, CategoryRepository categories,
                                     AccountRepository accounts, IncomeRepository incomes, ExpenseRepository expenses) {
        this.users=users; this.finance=finance; this.categories=categories; this.accounts=accounts; this.incomes=incomes; this.expenses=expenses;
    }
    @GetMapping("/statements/import") public String page(){return "statement-import";}

    private record Row(LocalDate date, String type, BigDecimal amount, Category category, String description, Account account) {}

    @PostMapping("/statements/import")
    @Transactional
    public String importCsv(@AuthenticationPrincipal UserDetails principal, @RequestParam("file") MultipartFile file, RedirectAttributes flash) {
        User user=users.findByUsername(principal.getUsername());
        try {
            if(file.isEmpty() || file.getSize()>2_000_000) throw new IllegalArgumentException("Choose a CSV file smaller than 2 MB.");
            List<String> lines;
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) { lines=reader.lines().toList(); }
            if(lines.size()<2) throw new IllegalArgumentException("The CSV needs a header and at least one transaction row.");
            List<String> header=parse(lines.get(0));
            Map<String,Integer> columns=new HashMap<>(); for(int i=0;i<header.size();i++) columns.put(header.get(i).trim().toLowerCase(Locale.ROOT),i);
            for(String required:List.of("date","type","amount","category","description")) if(!columns.containsKey(required)) throw new IllegalArgumentException("Missing required CSV column: "+required);
            if(lines.size()>1001) throw new IllegalArgumentException("Import up to 1,000 transactions at a time.");
            List<Row> rows=new ArrayList<>();
            for(int i=1;i<lines.size();i++) {
                final int rowNumber=i+1;
                if(lines.get(i).isBlank()) continue;
                List<String> cells=parse(lines.get(i));
                String type=cell(cells,columns,"type").trim().toUpperCase(Locale.ROOT);
                Category.Type categoryType=switch(type){case "INCOME"->Category.Type.INCOME;case "EXPENSE"->Category.Type.EXPENSE;default->throw new IllegalArgumentException("Row "+(i+1)+": type must be income or expense.");};
                String categoryName=cell(cells,columns,"category").trim();
                Category category=categories.findByType(categoryType).stream().filter(c->c.getName().equalsIgnoreCase(categoryName)).findFirst().orElseThrow(()->new IllegalArgumentException("Row "+rowNumber+": category not found: "+categoryName));
                String accountName=columns.containsKey("account")?cell(cells,columns,"account").trim():"";
                Account account=accountName.isBlank()?null:accounts.findByUserId(user.getId()).stream().filter(a->a.getAccountName().equalsIgnoreCase(accountName)).findFirst().orElseThrow(()->new IllegalArgumentException("Row "+rowNumber+": account not found: "+accountName));
                BigDecimal amount=new BigDecimal(cell(cells,columns,"amount").trim()); if(amount.signum()<=0) throw new IllegalArgumentException("Row "+(i+1)+": amount must be greater than zero.");
                rows.add(new Row(LocalDate.parse(cell(cells,columns,"date").trim()),type,amount,category,cell(cells,columns,"description").trim(),account));
            }
            if(rows.isEmpty()) throw new IllegalArgumentException("No transaction rows were found.");
            for(Row row:rows) if(row.type().equals("EXPENSE")) {
                Expense e=finance.addExpense(user,row.category(),row.amount(),row.description(),row.date());
                if(row.account()!=null){e.setAccount(row.account());row.account().setBalance(row.account().getBalance().subtract(row.amount()));accounts.save(row.account());}
                expenses.save(e);
            } else {
                Income in=finance.addIncome(user,row.category(),row.amount(),row.description(),row.date());
                if(row.account()!=null){in.setAccount(row.account());row.account().setBalance(row.account().getBalance().add(row.amount()));accounts.save(row.account());}
                incomes.save(in);
            }
            flash.addFlashAttribute("importSuccess",rows.size()+" transactions imported.");
        } catch(Exception ex) { flash.addFlashAttribute("importError",ex instanceof IllegalArgumentException?ex.getMessage():"Could not read the CSV. Check its dates, amounts, and column format."); }
        return "redirect:/statements/import";
    }
    private String cell(List<String> cells,Map<String,Integer> columns,String key) { int index=columns.get(key); if(index>=cells.size()) throw new IllegalArgumentException("A CSV row is missing the "+key+" value."); return cells.get(index); }
    private List<String> parse(String line) {
        List<String> result=new ArrayList<>(); StringBuilder value=new StringBuilder(); boolean quoted=false;
        for(int i=0;i<line.length();i++){char c=line.charAt(i);if(c=='"'){if(quoted&&i+1<line.length()&&line.charAt(i+1)=='"'){value.append('"');i++;}else quoted=!quoted;}else if(c==','&&!quoted){result.add(value.toString());value.setLength(0);}else value.append(c);}
        if(quoted) throw new IllegalArgumentException("The CSV contains an unclosed quote.");result.add(value.toString());return result;
    }
}
