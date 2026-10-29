package com.acme.salary.seed;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Builds a realistic, reproducible workforce: headcount weighted by country and department,
 * pay bands in local currency scaled by department and seniority, and a salary history that
 * starts at hire and gets a raise on every work anniversary.
 */
class SeedDataGenerator {

    /** {@code midSalary}: typical annual pay for a mid-level engineer in that country, local currency. */
    private record Country(String code, String currency, long midSalary, String phonePrefix, int weight) {}

    private record Department(String name, double payFactor, int weight, String... titles) {}

    private record Level(String prefix, double payFactor, int weight) {}

    private static final List<Country> COUNTRIES = List.of(
            new Country("IN", "INR", 1_800_000, "+91", 30),
            new Country("US", "USD", 120_000, "+1", 25),
            new Country("GB", "GBP", 65_000, "+44", 10),
            new Country("DE", "EUR", 70_000, "+49", 8),
            new Country("CA", "CAD", 100_000, "+1", 8),
            new Country("SG", "SGD", 95_000, "+65", 7),
            new Country("AU", "AUD", 115_000, "+61", 6),
            new Country("JP", "JPY", 9_000_000, "+81", 6));

    private static final List<Department> DEPARTMENTS = List.of(
            new Department("Engineering", 1.00, 35, "Software Engineer", "QA Engineer", "DevOps Engineer", "Data Engineer"),
            new Department("Product", 1.00, 8, "Product Manager", "Product Designer"),
            new Department("Sales", 0.85, 15, "Account Executive", "Sales Development Representative"),
            new Department("Marketing", 0.80, 8, "Marketing Specialist", "Content Strategist"),
            new Department("Finance", 0.90, 8, "Financial Analyst", "Accountant"),
            new Department("People", 0.75, 6, "HR Generalist", "Recruiter"),
            new Department("Operations", 0.70, 20, "Operations Analyst", "Support Specialist"));

    private static final List<Level> LEVELS = List.of(
            new Level("Junior ", 0.70, 25),
            new Level("", 1.00, 40),
            new Level("Senior ", 1.35, 25),
            new Level("Lead ", 1.70, 10));

    private static final String[] FIRST_NAMES = {
            "Aarav", "Aditi", "Akira", "Amelia", "Ananya", "Arjun", "Ben", "Chloe", "Daniel", "Divya",
            "Elena", "Emma", "Ethan", "Fatima", "Felix", "Grace", "Hana", "Isha", "Jack", "Kabir",
            "Kenji", "Lena", "Liam", "Lucas", "Maya", "Mei", "Mia", "Noah", "Olivia", "Priya",
            "Rahul", "Riya", "Rohan", "Sakura", "Sara", "Sofia", "Tom", "Vikram", "Wei", "Zoe"};

    private static final String[] LAST_NAMES = {
            "Agarwal", "Brown", "Chen", "Clarke", "Das", "Fischer", "Garcia", "Gupta", "Hoffmann", "Iyer",
            "Johnson", "Kapoor", "Kim", "Kumar", "Lee", "Lim", "Martin", "Mehta", "Miller", "Muller",
            "Nair", "Nguyen", "Patel", "Rao", "Reddy", "Sato", "Schmidt", "Shah", "Sharma", "Singh",
            "Smith", "Suzuki", "Tan", "Taylor", "Thomas", "Verma", "Wang", "Williams", "Wilson", "Wong"};

    private static final int MAX_TENURE_DAYS = 365 * 10;
    private static final double TERMINATION_RATE = 0.06;

    private final Random random;

    SeedDataGenerator(long seed) {
        this.random = new Random(seed);
    }

    List<SeedEmployee> generate(int count, LocalDate today) {
        List<SeedEmployee> employees = new ArrayList<>(count);
        Map<String, Integer> emailCounts = new HashMap<>();
        for (int i = 1; i <= count; i++) {
            employees.add(employee(i, today, emailCounts));
        }
        return employees;
    }

    private SeedEmployee employee(int n, LocalDate today, Map<String, Integer> emailCounts) {
        Country country = pick(COUNTRIES, Country::weight);
        Department department = pick(DEPARTMENTS, Department::weight);
        Level level = pick(LEVELS, Level::weight);
        String title = level.prefix() + department.titles()[random.nextInt(department.titles().length)];

        String first = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String last = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        String emailBase = (first + "." + last).toLowerCase();
        int seen = emailCounts.merge(emailBase, 1, Integer::sum);
        String email = (seen == 1 ? emailBase : emailBase + seen) + "@acme.com";

        LocalDate hireDate = today.minusDays(random.nextInt(MAX_TENURE_DAYS));
        long tenureDays = ChronoUnit.DAYS.between(hireDate, today);
        LocalDate terminationDate = tenureDays >= 90 && random.nextDouble() < TERMINATION_RATE
                ? hireDate.plusDays(90 + random.nextInt((int) tenureDays - 89))
                : null;

        double target = country.midSalary() * department.payFactor() * level.payFactor() * (0.9 + random.nextDouble() * 0.2);
        List<SeedSalary> salaries = salaryHistory(target, country.currency(), hireDate,
                terminationDate != null ? terminationDate : today);

        String phone = random.nextDouble() < 0.9
                ? country.phonePrefix() + " " + (100_000_000 + random.nextInt(900_000_000))
                : null;

        return new SeedEmployee("EMP-%05d".formatted(n), first + " " + last, title, department.name(),
                country.code(), email, phone, hireDate, terminationDate, salaries);
    }

    /** Starts below {@code target} and compounds ~5% anniversary raises so long-tenured staff land near it. */
    private List<SeedSalary> salaryHistory(double target, String currency, LocalDate hireDate, LocalDate lastDay) {
        int anniversaries = (int) ChronoUnit.YEARS.between(hireDate, lastDay);
        double amount = target / Math.pow(1.05, anniversaries);
        List<SeedSalary> history = new ArrayList<>(anniversaries + 1);
        history.add(new SeedSalary(money(amount), currency, hireDate));
        for (int year = 1; year <= anniversaries; year++) {
            amount *= 1.02 + random.nextDouble() * 0.06;
            history.add(new SeedSalary(money(amount), currency, hireDate.plusYears(year)));
        }
        return history;
    }

    private static BigDecimal money(double amount) {
        return BigDecimal.valueOf(Math.round(amount)).setScale(2, RoundingMode.UNNECESSARY);
    }

    private <T> T pick(List<T> options, java.util.function.ToIntFunction<T> weight) {
        int total = options.stream().mapToInt(weight).sum();
        int roll = random.nextInt(total);
        for (T option : options) {
            roll -= weight.applyAsInt(option);
            if (roll < 0) {
                return option;
            }
        }
        throw new IllegalStateException("unreachable");
    }
}
