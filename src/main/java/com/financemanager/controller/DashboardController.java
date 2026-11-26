package com.financemanager.controller;

import com.financemanager.entity.*;
import com.financemanager.repository.*;
import com.financemanager.entity.WishVaultItem;
import com.financemanager.service.FinanceService;
import com.financemanager.service.UserService;
import com.financemanager.service.FinancialHealthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Controller
public class DashboardController {
    private final FinanceService finance;
    private final UserService users;
    private final FinancialHealthService health;
    private final SavingsGoalRepository goals;
    private final AccountRepository accounts;
    private final TransactionSplitRepository splits;
    private final CategoryRepository categories;
    private final RecurringBillRepository recurringBills;
    private final ExpenseRepository expenses;
    private final NotificationRepository notifications;
    private final WishVaultRepository wishes;

    public DashboardController(FinanceService finance, UserService users, FinancialHealthService health,
                               SavingsGoalRepository goals, AccountRepository accounts, TransactionSplitRepository splits, CategoryRepository categories, RecurringBillRepository recurringBills, ExpenseRepository expenses, NotificationRepository notifications, WishVaultRepository wishes) {
        this.finance = finance; this.users = users; this.health = health; this.goals = goals;
        this.accounts = accounts; this.splits = splits; this.categories = categories; this.recurringBills=recurringBills; this.expenses=expenses; this.notifications=notifications; this.wishes=wishes;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = users.findByUsername(principal.getUsername());
        YearMonth month = YearMonth.now();
        LocalDate today = LocalDate.now();
        BigDecimal income = finance.getMonthlyIncome(user.getId(), month);
        BigDecimal expense = finance.getMonthlyExpense(user.getId(), month);
        BigDecimal savings = income.subtract(expense);
        int daysLeft = Math.max(1, month.lengthOfMonth() - today.getDayOfMonth() + 1);
        List<Account> accountRows = accounts.findByUserIdOrderByMemberNameAscAccountNameAsc(user.getId());
        BigDecimal liquidity = BigDecimal.ZERO;
        // Account balances are stored as signed amounts; card purchases are recorded as negative balances.
        for (Account a : accountRows) liquidity = liquidity.add(a.getBalance());
        BigDecimal monthlyCaps = finance.getBudgets(user.getId()).stream().map(Budget::getMonthlyLimit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal spendable = monthlyCaps.signum() > 0 ? monthlyCaps.subtract(expense).max(BigDecimal.ZERO)
                : income.multiply(new BigDecimal("0.50")).subtract(expense).max(BigDecimal.ZERO);

        model.addAttribute("user", user);
        model.addAttribute("healthScore", health.score(user.getId()));
        model.addAttribute("savingsGoals", goals.findByUserId(user.getId()));
        model.addAttribute("wishItems", wishes.findByUserIdAndStatusInOrderByExpiresAtAsc(user.getId(), List.of(WishVaultItem.Status.WAITING, WishVaultItem.Status.READY)));
        model.addAttribute("netBalance", finance.getNetBalance(user.getId()));
        model.addAttribute("trackedLiquidity", liquidity);
        model.addAttribute("monthlyIncome", income);
        model.addAttribute("monthlyExpense", expense);
        model.addAttribute("monthlySavings", savings);
        model.addAttribute("savingsRate", income.signum() == 0 ? BigDecimal.ZERO : savings.multiply(BigDecimal.valueOf(100)).divide(income, 1, RoundingMode.HALF_UP));
        model.addAttribute("dailySafeSpend", spendable.divide(BigDecimal.valueOf(daysLeft), 2, RoundingMode.HALF_UP));
        model.addAttribute("daysLeftInMonth", month.lengthOfMonth() - today.getDayOfMonth());
        model.addAttribute("currentMonthLabel", month.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH));
        model.addAttribute("monthProgressPercent", today.getDayOfMonth() * 100 / month.lengthOfMonth());
        model.addAttribute("incomeHistory", finance.getIncomeHistory(user.getId()));
        model.addAttribute("expenseHistory", finance.getExpenseHistory(user.getId()));
        model.addAttribute("accounts", accountRows);
        model.addAttribute("upcomingBills", recurringBills.findByUserIdAndActiveTrueAndNextDueDateBetweenOrderByNextDueDateAsc(user.getId(), today, today.plusDays(14)));
        model.addAttribute("budgets", finance.getBudgets(user.getId()));

        List<Map<String,Object>> budgetStats = new ArrayList<>();
        for (Budget b : finance.getBudgets(user.getId())) {
            BigDecimal used = splits.sumByUserAndCategoryAndDateRange(user.getId(), b.getCategory().getId(), month.atDay(1), month.atEndOfMonth());
            int pct = b.getMonthlyLimit().signum() == 0 ? 0 : used.multiply(BigDecimal.valueOf(100)).divide(b.getMonthlyLimit(), 0, RoundingMode.HALF_UP).intValue();
            budgetStats.add(Map.of("name", b.getCategory().getName(), "spent", used, "limit", b.getMonthlyLimit(), "percent", Math.min(100, Math.max(0,pct)), "actualPercent", pct, "tone", pct >= 100 ? "danger" : pct >= 75 ? "warning" : "success"));
        }
        model.addAttribute("budgetStats", budgetStats);

        List<Map<String,Object>> categoryStats = new ArrayList<>();
        BigDecimal categorized = BigDecimal.ZERO;
        for (Category c : userExpenseCategories(user.getId())) categorized = categorized.add(splits.sumByUserAndCategoryAndDateRange(user.getId(), c.getId(), month.atDay(1), month.atEndOfMonth()));
        for (Category c : userExpenseCategories(user.getId())) {
            BigDecimal total = splits.sumByUserAndCategoryAndDateRange(user.getId(), c.getId(), month.atDay(1), month.atEndOfMonth());
            if (total.signum() > 0) categoryStats.add(Map.of("name", c.getName(), "amount", total, "percent", categorized.signum()==0?0:total.multiply(BigDecimal.valueOf(100)).divide(categorized,0,RoundingMode.HALF_UP).intValue()));
        }
        categoryStats.sort((a,b)->((BigDecimal)b.get("amount")).compareTo((BigDecimal)a.get("amount")));
        String[] colors={"#819f64","#d39a75","#668873","#b6a06e","#8296a3","#aa7d74","#a7b88a"};
        StringBuilder gradient=new StringBuilder("conic-gradient(");
        BigDecimal position=BigDecimal.ZERO;
        for(int i=0;i<categoryStats.size();i++) {
            BigDecimal amount=(BigDecimal)categoryStats.get(i).get("amount");
            BigDecimal next=position.add(categorized.signum()==0?BigDecimal.ZERO:amount.multiply(BigDecimal.valueOf(100)).divide(categorized,2,RoundingMode.HALF_UP));
            if(i>0) gradient.append(", ");
            gradient.append(colors[i%colors.length]).append(' ').append(position).append("% ").append(next).append('%');
            position=next;
        }
        gradient.append(')');
        model.addAttribute("categoryGradient",gradient.toString());
        model.addAttribute("allCategoryStats",categoryStats);
        model.addAttribute("categoryStats", categoryStats.stream().limit(6).toList());
        model.addAttribute("categoryTotal", categorized);

        LocalDate heatmapStart = today.minusDays(181);
        Map<LocalDate, BigDecimal> heatmapTotals = new HashMap<>();
        for (Object[] row : expenses.dailyTotals(user.getId(), heatmapStart, today)) {
            heatmapTotals.put((LocalDate) row[0], (BigDecimal) row[1]);
        }
        BigDecimal heatmapMax = heatmapTotals.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        List<Map<String, Object>> spendingHeatmap = new ArrayList<>();
        for (LocalDate day = heatmapStart; !day.isAfter(today); day = day.plusDays(1)) {
            BigDecimal amount = heatmapTotals.getOrDefault(day, BigDecimal.ZERO);
            int level = amount.signum() == 0 || heatmapMax.signum() == 0 ? 0 :
                    Math.min(4, Math.max(1, amount.multiply(BigDecimal.valueOf(4)).divide(heatmapMax, 0, RoundingMode.CEILING).intValue()));
            spendingHeatmap.add(Map.of("date", day, "amount", amount, "level", level));
        }
        model.addAttribute("spendingHeatmap", spendingHeatmap);
        model.addAttribute("heatmapStart", heatmapStart);

        List<Map<String,Object>> daily = new ArrayList<>();
        for (int offset = 13; offset >= 0; offset--) {
            LocalDate date = today.minusDays(offset);
            BigDecimal in = finance.getIncomeForDay(user.getId(), date);
            BigDecimal out = finance.getExpenseForDay(user.getId(), date);
            BigDecimal max = in.max(out);
            int incomeH = max.signum() == 0 ? 2 : Math.max(3, in.multiply(BigDecimal.valueOf(100)).divide(max, 0, RoundingMode.DOWN).intValue());
            int expenseH = max.signum() == 0 ? 2 : Math.max(3, out.multiply(BigDecimal.valueOf(100)).divide(max, 0, RoundingMode.DOWN).intValue());
            daily.add(Map.of("label", String.valueOf(date.getDayOfMonth()), "income", in, "expense", out, "max", max, "incomeH", incomeH, "expenseH", expenseH));
        }
        model.addAttribute("dailyFlow", daily);

        YearMonth previous = month.minusMonths(1);
        List<Map<String,Object>> comparisons = new ArrayList<>();
        for (Category c : userExpenseCategories(user.getId())) {
            BigDecimal now = splits.sumByUserAndCategoryAndDateRange(user.getId(), c.getId(), month.atDay(1), month.atEndOfMonth());
            BigDecimal before = splits.sumByUserAndCategoryAndDateRange(user.getId(), c.getId(), previous.atDay(1), previous.atEndOfMonth());
            if (now.signum()>0 || before.signum()>0) {
                BigDecimal scale = now.max(before);
                int prevW = scale.signum() == 0 ? 0 : before.multiply(BigDecimal.valueOf(100)).divide(scale, 0, RoundingMode.DOWN).intValue();
                int currW = scale.signum() == 0 ? 0 : now.multiply(BigDecimal.valueOf(100)).divide(scale, 0, RoundingMode.DOWN).intValue();
                comparisons.add(Map.of("name", c.getName(), "current", now, "previous", before, "scale", scale, "prevW", prevW, "currW", currW));
            }
        }
        comparisons.sort((a,b)->((BigDecimal)b.get("current")).compareTo((BigDecimal)a.get("current")));
        model.addAttribute("comparisons", comparisons.stream().limit(5).toList());

        List<Map<String,Object>> recent = new ArrayList<>();
        for (Expense e : finance.getExpenseHistory(user.getId())) recent.add(Map.of("date",e.getDate(),"title",Optional.ofNullable(e.getDescription()).filter(s->!s.isBlank()).orElse(e.getCategory().getName()),"category",e.getCategory().getName(),"account",e.getAccount()==null?"Unassigned":e.getAccount().getAccountName(),"amount",e.getAmount(),"type","Expense","sign","−"));
        for (Income i : finance.getIncomeHistory(user.getId())) recent.add(Map.of("date",i.getDate(),"title",Optional.ofNullable(i.getDescription()).filter(s->!s.isBlank()).orElse(i.getCategory().getName()),"category",i.getCategory().getName(),"account",i.getAccount()==null?"Unassigned":i.getAccount().getAccountName(),"amount",i.getAmount(),"type","Income","sign","+"));
        recent.sort((a,b)->((LocalDate)b.get("date")).compareTo((LocalDate)a.get("date")));
        model.addAttribute("recentTransactions", recent.stream().limit(8).toList());
        List<String> alerts = new ArrayList<>(budgetStats.stream().filter(b->(int)b.get("actualPercent")>=100).map(b->"Budget exceeded: " + b.get("name") + " is over its monthly limit.").toList());
        notifications.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().limit(3)
                .map(Notification::getMessage).forEach(alerts::add);
        LocalDate windowStart=today.minusDays(29), priorStart=windowStart.minusDays(30), priorEnd=windowStart.minusDays(1);
        for(Category c:userExpenseCategories(user.getId())) {
            BigDecimal recentSpend=splits.sumByUserAndCategoryAndDateRange(user.getId(),c.getId(),windowStart,today);
            BigDecimal priorSpend=splits.sumByUserAndCategoryAndDateRange(user.getId(),c.getId(),priorStart,priorEnd);
            if(priorSpend.compareTo(BigDecimal.ZERO)>0 && recentSpend.compareTo(priorSpend.multiply(new BigDecimal("1.5")))>0)
                alerts.add("Spending in "+c.getName()+" is up more than 50% compared with the previous 30 days.");
        }
        model.addAttribute("alerts", alerts.stream().limit(4).toList());
        BigDecimal needs=BigDecimal.ZERO,wants=BigDecimal.ZERO;
        for (Map<String,Object> row : categoryStats) { String name=((String)row.get("name")).toLowerCase(Locale.ROOT); BigDecimal amount=(BigDecimal)row.get("amount"); if (List.of("rent","housing","home","utility","utilities","grocer","food","transport","health","medical","education","child").stream().anyMatch(name::contains)) needs=needs.add(amount); else wants=wants.add(amount); }
        model.addAttribute("needsPercent", income.signum()==0?0:needs.multiply(BigDecimal.valueOf(100)).divide(income,0,RoundingMode.HALF_UP).intValue());
        model.addAttribute("wantsPercent", income.signum()==0?0:wants.multiply(BigDecimal.valueOf(100)).divide(income,0,RoundingMode.HALF_UP).intValue());
        model.addAttribute("savingsBenchmark", income.signum()==0?0:savings.max(BigDecimal.ZERO).multiply(BigDecimal.valueOf(100)).divide(income,0,RoundingMode.HALF_UP).intValue());
        int needsPct  = income.signum()==0?0:needs.multiply(BigDecimal.valueOf(100)).divide(income,0,RoundingMode.HALF_UP).intValue();
        int wantsPct  = income.signum()==0?0:wants.multiply(BigDecimal.valueOf(100)).divide(income,0,RoundingMode.HALF_UP).intValue();
        int savingsPct= income.signum()==0?0:savings.max(BigDecimal.ZERO).multiply(BigDecimal.valueOf(100)).divide(income,0,RoundingMode.HALF_UP).intValue();
        model.addAttribute("needsBarW",   Math.min(100, needsPct  * 2));
        model.addAttribute("wantsBarW",   Math.min(100, wantsPct  * 3));
        model.addAttribute("savingsBarW", Math.min(100, savingsPct * 5));

        model.addAttribute("moneyMoments", List.of(
                Map.of("tag", "YOUR MONTH, SO FAR", "title", savings.signum()>0?"You’ve got some breathing room.":"Take a fresh look at this month.", "copy", savings.signum()>0?"Your recorded income is ahead of expenses. Consider putting a little toward one of your goals.":"Review your spending and budget to decide what comes next.", "url", savings.signum()<0?"/budgets":"/dashboard#goal-form", "action", savings.signum()<0?"Review your budgets":"Visit a savings goal", "symbol", "✳"),
                Map.of("tag", "A TWO-MINUTE MONEY RESET", "title", "Find one quiet leak.", "copy", "Look through the last few purchases for a subscription or repeat spend you no longer use.", "url", "/transactions", "action", "Review recent activity", "symbol", "⌕"),
                Map.of("tag", "A HABIT THAT ADDS UP", "title", "Give your next goal a name.", "copy", "A clear target makes it easier to notice small steps toward the things you care about.", "url", "/dashboard#goal-form", "action", "Set a savings goal", "symbol", "↗")));
        return "dashboard";
    }

    private List<Category> userExpenseCategories(Long userId) {
        // Categories are application-wide seeded values; only EXPENSE categories belong in the breakdown.
        return categories.findByType(Category.Type.EXPENSE);
    }
}
