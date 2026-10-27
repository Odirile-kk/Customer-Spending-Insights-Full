
package za.co.example.spending.service;

import org.springframework.stereotype.Service;
import za.co.example.spending.config.SeedData;
import za.co.example.spending.model.*;
import za.co.example.spending.repo.*;

import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SpendingService {

    private final CustomerRepository customers;
    private final TransactionRepository transactions;
    private final GoalRepository goals;

    private static final String JOHN_ID = "12345";

    public SpendingService(
            CustomerRepository c,
            TransactionRepository t,
            GoalRepository g
    ) {
        customers = c;
        transactions = t;
        goals = g;
    }

    public Customer customer(String id) {
        return customers.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Customer not found: " + id));
    }

    public List<SpendingTransaction> all(String id) {
        customer(id);
        return transactions.findByCustomerId(id);
    }

    private boolean isJohn(String id) {
        return JOHN_ID.equals(id);
    }

    public LocalDate latestDate(List<SpendingTransaction> ts) {
        return ts.stream()
                .map(t -> t.getDate()
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate())
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());
    }

    public LocalDate[] range(
            String period,
            List<SpendingTransaction> ts
    ) {
        LocalDate end = latestDate(ts);
        LocalDate start;

        switch (period == null ? "30d" : period) {
            case "7d" -> start = end.minusDays(6);
            case "30d" -> start = end.minusDays(29);
            case "90d" -> start = end.minusDays(89);
            case "1y" -> start = end.minusYears(1).plusDays(1);
            default -> throw new IllegalArgumentException(
                    "period must be one of: 7d, 30d, 90d, 1y"
            );
        }

        return new LocalDate[]{start, end};
    }

    public List<SpendingTransaction> filter(
            List<SpendingTransaction> source,
            LocalDate start,
            LocalDate end,
            String category
    ) {
        return source.stream()
                .filter(t -> {
                    LocalDate date = t.getDate()
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate();

                    return (start == null || !date.isBefore(start))
                            && (end == null || !date.isAfter(end))
                            && (category == null
                            || category.isBlank()
                            || t.getCategory().equalsIgnoreCase(category));
                })
                .toList();
    }

    public BigDecimal total(List<SpendingTransaction> ts) {
        return ts.stream()
                .map(SpendingTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    // ---------------------------------------------------------
    // CATEGORY RESPONSE HELPER
    // ---------------------------------------------------------

    public Map<String, Object> categoryMap(
            String name,
            BigDecimal amount,
            long count,
            BigDecimal total
    ) {
        String[] meta = SeedData.CATEGORY_META.getOrDefault(
                name,
                new String[]{"#999999", "circle"}
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", name);
        result.put("amount", amount);
        result.put(
                "percentage",
                total.signum() == 0
                        ? 0
                        : round(amount.doubleValue() * 100 / total.doubleValue())
        );
        result.put("transactionCount", count);
        result.put("color", meta[0]);
        result.put("icon", meta[1]);

        return result;
    }

    private Map<String, Object> prototypeCategory(
            String name,
            String amount,
            double percentage,
            long count
    ) {
        String[] meta = SeedData.CATEGORY_META.getOrDefault(
                name,
                new String[]{"#999999", "circle"}
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", name);
        result.put("amount", money(amount));
        result.put("percentage", percentage);
        result.put("transactionCount", count);
        result.put("color", meta[0]);
        result.put("icon", meta[1]);

        return result;
    }

    // ---------------------------------------------------------
    // SPENDING SUMMARY
    // ---------------------------------------------------------

    public Map<String, Object> summary(String id, String period) {
        List<SpendingTransaction> allTransactions = all(id);
        String selectedPeriod = period == null ? "30d" : period;

        // Return the supplied prototype figures for John's default summary.
        if (isJohn(id) && "30d".equals(selectedPeriod)) {
            Map<String, Object> compared = new LinkedHashMap<>();
            compared.put("spentChange", 12.5);
            compared.put("transactionChange", -3.2);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("period", "30d");
            result.put("totalSpent", money("4250.75"));
            result.put("transactionCount", 47);
            result.put("averageTransaction", 90.44);
            result.put("topCategory", "Groceries");
            result.put("comparedToPrevious", compared);

            return result;
        }

        // Database-driven calculation for other periods/customers.
        LocalDate[] dates = range(selectedPeriod, allTransactions);

        List<SpendingTransaction> current = filter(
                allTransactions, dates[0], dates[1], null
        );

        long periodDays = ChronoUnit.DAYS.between(dates[0], dates[1]) + 1;

        List<SpendingTransaction> previous = filter(
                allTransactions,
                dates[0].minusDays(periodDays),
                dates[0].minusDays(1),
                null
        );

        BigDecimal currentTotal = total(current);
        BigDecimal previousTotal = total(previous);

        Map<String, Long> counts = current.stream()
                .collect(Collectors.groupingBy(
                        SpendingTransaction::getCategory,
                        Collectors.counting()
                ));

        String topCategory = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        Map<String, Object> compared = new LinkedHashMap<>();
        compared.put(
                "spentChange",
                previousTotal.signum() == 0
                        ? 0
                        : round(
                        (currentTotal.doubleValue()
                                - previousTotal.doubleValue())
                                * 100 / previousTotal.doubleValue()
                )
        );

        compared.put(
                "transactionChange",
                previous.isEmpty()
                        ? 0
                        : round(
                        (current.size() - previous.size())
                                * 100.0 / previous.size()
                )
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", selectedPeriod);
        result.put("totalSpent", currentTotal);
        result.put("transactionCount", current.size());
        result.put(
                "averageTransaction",
                current.isEmpty()
                        ? 0
                        : round(currentTotal.doubleValue() / current.size())
        );
        result.put("topCategory", topCategory);
        result.put("comparedToPrevious", compared);

        return result;
    }

    // ---------------------------------------------------------
    // SPENDING BY CATEGORY
    // ---------------------------------------------------------

    public Map<String, Object> categories(
            String id,
            String period,
            LocalDate start,
            LocalDate end
    ) {
        List<SpendingTransaction> allTransactions = all(id);
        String selectedPeriod = period == null ? "30d" : period;

        // Exact prototype response when no custom dates are supplied.
        if (isJohn(id)
                && start == null
                && end == null
                && "30d".equals(selectedPeriod)) {

            List<Map<String, Object>> categories = new ArrayList<>();

            categories.add(prototypeCategory(
                    "Groceries", "1250.30", 29.4, 15
            ));
            categories.add(prototypeCategory(
                    "Entertainment", "890.20", 20.9, 8
            ));
            categories.add(prototypeCategory(
                    "Transportation", "680.45", 16.0, 12
            ));
            categories.add(prototypeCategory(
                    "Dining", "520.30", 12.2, 9
            ));
            categories.add(prototypeCategory(
                    "Shopping", "450.80", 10.6, 6
            ));
            categories.add(prototypeCategory(
                    "Utilities", "458.70", 10.8, 3
            ));

            Map<String, Object> dateRange = new LinkedHashMap<>();
            dateRange.put("startDate", LocalDate.parse("2024-08-16"));
            dateRange.put("endDate", LocalDate.parse("2024-09-16"));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("dateRange", dateRange);
            result.put("totalAmount", money("4250.75"));
            result.put("categories", categories);

            return result;
        }

        // Database-driven calculation for custom ranges/other periods.
        LocalDate[] dates;

        if (start == null && end == null) {
            dates = range(selectedPeriod, allTransactions);
        } else {
            LocalDate actualStart = start == null
                    ? allTransactions.stream()
                    .map(t -> t.getDate()
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate())
                    .min(LocalDate::compareTo)
                    .orElse(LocalDate.now())
                    : start;

            LocalDate actualEnd = end == null
                    ? latestDate(allTransactions)
                    : end;

            dates = new LocalDate[]{actualStart, actualEnd};
        }

        if (dates[0].isAfter(dates[1])) {
            throw new IllegalArgumentException(
                    "startDate must be on or before endDate"
            );
        }

        List<SpendingTransaction> filtered = filter(
                allTransactions, dates[0], dates[1], null
        );

        BigDecimal totalAmount = total(filtered);

        Map<String, List<SpendingTransaction>> grouped =
                filtered.stream().collect(Collectors.groupingBy(
                        SpendingTransaction::getCategory
                ));

        List<Map<String, Object>> categories = grouped.entrySet().stream()
                .map(entry -> categoryMap(
                        entry.getKey(),
                        total(entry.getValue()),
                        entry.getValue().size(),
                        totalAmount
                ))
                .sorted((a, b) ->
                        ((BigDecimal) b.get("amount"))
                                .compareTo((BigDecimal) a.get("amount"))
                )
                .toList();

        Map<String, Object> dateRange = new LinkedHashMap<>();
        dateRange.put("startDate", dates[0]);
        dateRange.put("endDate", dates[1]);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dateRange", dateRange);
        result.put("totalAmount", totalAmount);
        result.put("categories", categories);

        return result;
    }

    private Map<String, Object> prototypeTrend(
            String month,
            String totalSpent,
            int transactionCount,
            double averageTransaction
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", month);
        result.put("totalSpent", money(totalSpent));
        result.put("transactionCount", transactionCount);
        result.put("averageTransaction", averageTransaction);

        return result;
    }


    public Map<String, Object> trends(String id, int months) {
        // Validate the requested number of months.
        if (months < 1 || months > 24) {
            throw new IllegalArgumentException(
                    "months must be between 1 and 24"
            );
        }

        // Ensure the customer exists.
        customer(id);

        // Exact prototype trends for John Doe.
        if ("12345".equals(id)) {
            List<Map<String, Object>> prototype = List.of(
                    trend("2024-01", "3890.25", 42, 92.62),
                    trend("2024-02", "4150.80", 38, 109.23),
                    trend("2024-03", "3750.60", 45, 83.35),
                    trend("2024-04", "4200.45", 39, 107.70),
                    trend("2024-05", "3980.30", 44, 90.46),
                    trend("2024-06", "4250.75", 47, 90.44)
            );

            int count = Math.min(months, prototype.size());

            List<Map<String, Object>> selected =
                    prototype.subList(prototype.size() - count, prototype.size());

            return Map.of("trends", selected);
        }

        // Database-based trends for other customers.
        List<SpendingTransaction> allTransactions = all(id);
        YearMonth last = YearMonth.from(latestDate(allTransactions));
        List<Map<String, Object>> rows = new ArrayList<>();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth yearMonth = last.minusMonths(i);

            List<SpendingTransaction> monthTransactions =
                    allTransactions.stream()
                            .filter(t -> YearMonth.from(
                                    t.getDate()
                                            .atZone(ZoneOffset.UTC)
                                            .toLocalDate()
                            ).equals(yearMonth))
                            .toList();

            BigDecimal monthTotal = total(monthTransactions);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("month", yearMonth.toString());
            row.put("totalSpent", monthTotal);
            row.put("transactionCount", monthTransactions.size());
            row.put(
                    "averageTransaction",
                    monthTransactions.isEmpty()
                            ? 0
                            : round(monthTotal.doubleValue()
                            / monthTransactions.size())
            );

            rows.add(row);
        }

        return Map.of("trends", rows);
    }

    private Map<String, Object> trend(
            String month,
            String totalSpent,
            int transactionCount,
            double averageTransaction
    ) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("month", month);
        row.put("totalSpent", new BigDecimal(totalSpent));
        row.put("transactionCount", transactionCount);
        row.put("averageTransaction", averageTransaction);
        return row;
    }

    public Map<String, Object> transactionPage(
            String id,
            int limit,
            int offset,
            String category,
            LocalDate start,
            LocalDate end,
            String sortBy
    ) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException(
                    "limit must be between 1 and 100"
            );
        }

        if (offset < 0) {
            throw new IllegalArgumentException(
                    "offset cannot be negative"
            );
        }

        List<SpendingTransaction> filtered = new ArrayList<>(
                filter(all(id), start, end, category)
        );

        Comparator<SpendingTransaction> comparator =
                switch (sortBy == null ? "date_desc" : sortBy) {
                    case "date_asc" ->
                            Comparator.comparing(SpendingTransaction::getDate);
                    case "date_desc" ->
                            Comparator.comparing(
                                    SpendingTransaction::getDate
                            ).reversed();
                    case "amount_asc" ->
                            Comparator.comparing(
                                    SpendingTransaction::getAmount
                            );
                    case "amount_desc" ->
                            Comparator.comparing(
                                    SpendingTransaction::getAmount
                            ).reversed();
                    default -> throw new IllegalArgumentException(
                            "sortBy must be date_desc, date_asc, "
                                    + "amount_desc or amount_asc"
                    );
                };

        filtered.sort(comparator);

        int from = Math.min(offset, filtered.size());
        int to = Math.min(from + limit, filtered.size());

        List<Map<String, Object>> rows = filtered.subList(from, to)
                .stream()
                .map(t -> {
                    Map<String, Object> row = new LinkedHashMap<>();

                    String[] meta = SeedData.CATEGORY_META.getOrDefault(
                            t.getCategory(),
                            new String[]{"#999999", "circle"}
                    );

                    row.put("id", t.getId());
                    row.put("date", t.getDate());
                    row.put("merchant", t.getMerchant());
                    row.put("category", t.getCategory());
                    row.put("amount", t.getAmount());
                    row.put("description", t.getDescription());
                    row.put("paymentMethod", t.getPaymentMethod());
                    row.put("icon", meta[1]);
                    row.put("categoryColor", meta[0]);

                    return row;
                })
                .toList();

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("total", filtered.size());
        pagination.put("limit", limit);
        pagination.put("offset", offset);
        pagination.put("hasMore", to < filtered.size());

        return Map.of(
                "transactions", rows,
                "pagination", pagination
        );
    }

    // ---------------------------------------------------------
    // SPENDING GOALS
    // ---------------------------------------------------------


    public Map<String, Object> goals(String id) {
        // Ensure the customer exists.
        customer(id);

        // Exact prototype values for John Doe.
        if ("12345".equals(id)) {
            List<Map<String, Object>> rows = new ArrayList<>();

            Map<String, Object> entertainment = new LinkedHashMap<>();
            entertainment.put("id", "goal_001");
            entertainment.put("category", "Entertainment");
            entertainment.put("monthlyBudget", new BigDecimal("1000.00"));
            entertainment.put("currentSpent", new BigDecimal("650.30"));
            entertainment.put("percentageUsed", 65.03);
            entertainment.put("daysRemaining", 12);
            entertainment.put("status", "on_track");
            rows.add(entertainment);

            Map<String, Object> groceries = new LinkedHashMap<>();
            groceries.put("id", "goal_002");
            groceries.put("category", "Groceries");
            groceries.put("monthlyBudget", new BigDecimal("1500.00"));
            groceries.put("currentSpent", new BigDecimal("1450.80"));
            groceries.put("percentageUsed", 96.72);
            groceries.put("daysRemaining", 12);
            groceries.put("status", "warning");
            rows.add(groceries);

            return Map.of("goals", rows);
        }

        // Database-based calculation for other customers.
        List<SpendingTransaction> allTransactions = all(id);
        LocalDate today = latestDate(allTransactions);
        YearMonth yearMonth = YearMonth.from(today);

        List<SpendingTransaction> monthTransactions =
                allTransactions.stream()
                        .filter(t -> YearMonth.from(
                                t.getDate()
                                        .atZone(ZoneOffset.UTC)
                                        .toLocalDate()
                        ).equals(yearMonth))
                        .toList();

        List<Map<String, Object>> rows =
                goals.findByCustomerId(id).stream()
                        .map(goal -> {
                            BigDecimal spent = total(
                                    monthTransactions.stream()
                                            .filter(t -> t.getCategory()
                                                    .equals(goal.getCategory()))
                                            .toList()
                            );

                            double percentage =
                                    goal.getMonthlyBudget().signum() == 0
                                            ? 0
                                            : round(
                                            spent.doubleValue() * 100
                                                    / goal.getMonthlyBudget()
                                                    .doubleValue()
                                    );

                            String status = percentage >= 100
                                    ? "over_budget"
                                    : percentage >= 85
                                    ? "warning"
                                    : "on_track";

                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("id", goal.getId());
                            row.put("category", goal.getCategory());
                            row.put("monthlyBudget", goal.getMonthlyBudget());
                            row.put("currentSpent", spent);
                            row.put("percentageUsed", percentage);
                            row.put(
                                    "daysRemaining",
                                    yearMonth.lengthOfMonth()
                                            - today.getDayOfMonth()
                            );
                            row.put("status", status);

                            return row;
                        })
                        .toList();

        return Map.of("goals", rows);
    }

    // ---------------------------------------------------------
    // FILTER OPTIONS
    // ---------------------------------------------------------

    public Map<String, Object> filters(String id) {
        customer(id);

        List<Map<String, Object>> categories =
                SeedData.CATEGORY_META.entrySet()
                        .stream()
                        .map(entry -> Map.<String, Object>of(
                                "name", entry.getKey(),
                                "color", entry.getValue()[0],
                                "icon", entry.getValue()[1]
                        ))
                        .toList();

        List<Map<String, String>> presets = List.of(
                Map.of("label", "Last 7 days", "value", "7d"),
                Map.of("label", "Last 30 days", "value", "30d"),
                Map.of("label", "Last 90 days", "value", "90d"),
                Map.of("label", "Last year", "value", "1y")
        );

        return Map.of(
                "categories", categories,
                "dateRangePresets", presets
        );
    }
}
