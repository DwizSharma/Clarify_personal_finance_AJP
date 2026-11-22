# Clarity · Personal Finance Manager

![Java](https://img.shields.io/badge/Java-17+-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?style=flat-square&logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square&logo=mysql)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)
![CI](https://github.com/DwizSharma/Clarify_personal_finance_AJP/actions/workflows/ci.yml/badge.svg)

A full-stack personal finance web app built with Spring Boot and Thymeleaf. Track income, expenses, budgets, savings goals, and upcoming bills — all from a clean dashboard.

---

## ✨ Features

| Feature | Description |
|---|---|
| 📊 **Dashboard** | Net worth, monthly income/expense, savings rate, daily safe-to-spend |
| 🔥 **Spending heatmap** | 26-week GitHub-style heatmap of daily outflows |
| 📈 **Cash flow chart** | 14-day income vs expense bar chart |
| 🎯 **Budget tracking** | Per-category monthly limits with real-time progress bars and alerts |
| 💰 **Savings goals** | Named goals with progress tracking and 72-hour wish vault |
| 🧾 **Split transactions** | Divide one receipt across multiple categories |
| 📂 **CSV import** | Bulk-import bank statements via CSV upload |
| 📤 **CSV export** | Export all transactions to CSV with date/type filters |
| 🔁 **Recurring bills** | Track upcoming rent, EMIs, subscriptions |
| 🏦 **Multi-account** | Track savings, checking, wallets, and credit cards per household member |
| 🔒 **Secure auth** | BCrypt password hashing, Spring Security session login, password change |
| 🌙 **Dark mode** | Toggle-able dark/light theme saved in localStorage |
| 📄 **PDF reports** | Download monthly PDF report |
| 🔍 **Transaction search** | Filter by keyword, type, category, and date range |

---

## 🛠 Tech Stack

- **Backend:** Java 17, Spring Boot 3.2.5, Spring MVC, Spring Security, Spring Data JPA / Hibernate
- **Frontend:** Thymeleaf, vanilla CSS, vanilla JavaScript
- **Database:** MySQL 8
- **Build:** Maven, embedded Tomcat
- **CI:** GitHub Actions

---

## 🚀 Quick Start

### Prerequisites
- JDK 17+
- Maven 3.8+
- MySQL 8

### Setup

```bash
# 1. Clone
git clone https://github.com/DwizSharma/Clarify_personal_finance_AJP.git
cd Clarify_personal_finance_AJP

# 2. Create database
mysql -u root -p < database/01_create_database.sql
mysql -u root -p < database/03_create_tables.sql

# 3. Set environment variables
export DB_USERNAME=root
export DB_PASSWORD=your_password

# 4. Run
mvn spring-boot:run
```

Then open **http://localhost:8082/register** to create an account.

### IntelliJ IDEA

1. **File → Open** the `project/` folder (the one containing `pom.xml`)
2. **File → Project Structure → Project SDK** → set to JDK 17+
3. **Run → Edit Configurations → + → Spring Boot**, main class: `com.financemanager.FinanceManagerApplication`
4. Add env vars: `DB_USERNAME=root`, `DB_PASSWORD=your_password`
5. Click **Run** → visit **http://localhost:8082**

---

## 🗄 Database Scripts

Run scripts from `database/` in this order:

| # | File | Purpose |
|---|---|---|
| 1 | `01_create_database.sql` | Create the schema |
| 2 | `02_repair_existing_users_table.sql` | Only if upgrading from an older version |
| 3 | `03_create_tables.sql` | Create all tables (safe to re-run) |
| 4 | `04_seed_categories.sql` | Seed default categories (app also does this on startup) |
| 5 | `05_verify.sql` | Optional sanity checks |

> `00_reset_database_OPTIONAL.sql` drops everything for a clean start.

---

## 📁 Project Structure

```
src/main/java/com/financemanager/
├── config/          # SecurityConfig, CategorySeeder, SplitBackfillRunner
├── controller/      # Dashboard, Expense, Income, Budget, Profile, CSV export...
├── entity/          # User, Expense, Income, Budget, Account, SavingsGoal...
├── repository/      # Spring Data JPA repositories
└── service/         # FinanceService, UserService, FinancialHealthService...

src/main/resources/
├── templates/       # Thymeleaf HTML templates + fragments/nav.html
└── static/css/      # style.css (design system)
```

---

## 🔑 Key Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/` | Landing page |
| GET/POST | `/register` | Create account |
| GET/POST | `/login` | Login |
| GET | `/dashboard` | Main dashboard |
| GET/POST | `/addExpense` | Log expense |
| GET/POST | `/addIncome` | Log income |
| GET | `/transactions` | Search & filter transactions |
| GET | `/transactions/export` | Export transactions as CSV |
| GET/POST | `/budgets` | Budget management |
| GET | `/profile` | Profile, accounts, password change |
| POST | `/transfers` | Transfer between accounts |
| GET | `/statements/import` | Import CSV statement |
| GET | `/reports/download?format=pdf` | Download monthly PDF |

---

## 📸 Screenshots

| Dashboard | Transactions | Budgets |
|---|---|---|
| *Coming soon* | *Coming soon* | *Coming soon* |

---

## 📝 Notes

- Passwords are hashed with BCrypt — never stored in plain text
- `ddl-auto=update` handles schema migrations in development; use Flyway/Liquibase for production
- Split transactions ensure correct per-category budget attribution
- All account balances are manually tracked — no live bank connections

---

## 📄 License

MIT © [DwizSharma](https://github.com/DwizSharma)
