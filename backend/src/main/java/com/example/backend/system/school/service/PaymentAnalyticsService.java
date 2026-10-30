package com.example.backend.system.school.service;

import com.example.backend.exception.ApiException;
import com.example.backend.system.school.dto.SchoolPaymentContracts.PackageRevenue;
import com.example.backend.system.school.dto.SchoolPaymentContracts.PaymentAnalytics;
import com.example.backend.system.school.dto.SchoolPaymentContracts.PaymentKpis;
import com.example.backend.system.school.dto.SchoolPaymentContracts.PaymentStatusBreakdown;
import com.example.backend.system.school.dto.SchoolPaymentContracts.RevenuePoint;
import com.example.backend.system.school.repository.SchoolPaymentRepository;
import com.example.backend.system.school.repository.SchoolPaymentRepository.PaymentAnalyticsRow;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentAnalyticsService {
    private static final String PAID = "PAID";
    private static final String PENDING = "PENDING";
    private static final String REVIEW = "REQUIRES_REVIEW";
    private static final String FAILED = "FAILED";
    private final SchoolPaymentRepository payments;

    public record PeriodRange(String period, int year, Integer month, Integer quarter, ZoneId zone,
                              Instant from, Instant to) { }

    @Transactional(readOnly = true)
    public PaymentAnalytics analytics(String period, Integer year, Integer month, Integer quarter, String timezone) {
        return analytics(period, year, month, quarter, timezone, null);
    }

    @Transactional(readOnly = true)
    public PaymentAnalytics analytics(String period, Integer year, Integer month, Integer quarter, String timezone, String date) {
        PeriodRange range = resolvePeriod(period, year, month, quarter, timezone, date);
        List<PaymentAnalyticsRow> rows = payments.findAnalyticsRows(range.from(), range.to());

        long grossPaidVnd = 0;
        long paidTransactions = 0;
        long pendingTransactions = 0;
        long reviewTransactions = 0;
        long failedTransactions = 0;
        Map<String, MutableRevenue> trend = emptyTrend(range);
        Map<String, MutableRevenue> packages = new LinkedHashMap<>();

        for (PaymentAnalyticsRow row : rows) {
            String status = normalizeStatus(row.getStatus());
            switch (status) {
                case PAID -> {
                    long amount = row.getAmountVnd() == null ? 0L : row.getAmountVnd();
                    grossPaidVnd = Math.addExact(grossPaidVnd, amount);
                    paidTransactions++;
                    Instant revenueAt = row.getPaidAt() == null ? row.getCreatedAt() : row.getPaidAt();
                    if (revenueAt != null) {
                        MutableRevenue bucket = trend.get(bucketKey(revenueAt, range));
                        if (bucket != null) bucket.add(amount);
                    }
                    packages.computeIfAbsent(row.getPlanCode() == null ? "Không xác định" : row.getPlanCode(), ignored -> new MutableRevenue()).add(amount);
                }
                case PENDING -> pendingTransactions++;
                case REVIEW -> reviewTransactions++;
                default -> failedTransactions++;
            }
        }

        List<RevenuePoint> points = trend.entrySet().stream()
                .map(entry -> new RevenuePoint(entry.getKey(), bucketLabel(entry.getKey(), range), entry.getValue().revenue, entry.getValue().transactions))
                .toList();
        List<PackageRevenue> packageRevenue = packages.entrySet().stream()
                .map(entry -> new PackageRevenue(entry.getKey(), entry.getValue().revenue, entry.getValue().transactions))
                .sorted(Comparator.comparingLong(PackageRevenue::revenueVnd).reversed().thenComparing(PackageRevenue::planCode))
                .toList();
        List<PaymentStatusBreakdown> statuses = List.of(
                new PaymentStatusBreakdown(PAID, paidTransactions),
                new PaymentStatusBreakdown(PENDING, pendingTransactions),
                new PaymentStatusBreakdown(REVIEW, reviewTransactions),
                new PaymentStatusBreakdown(FAILED, failedTransactions));
        PaymentKpis kpis = new PaymentKpis(grossPaidVnd, paidTransactions, pendingTransactions,
                reviewTransactions, failedTransactions, rows.size());

        return new PaymentAnalytics(range.period(), range.year(), range.month(), range.quarter(),
                range.zone().getId(), range.from(), range.to(), kpis, points, packageRevenue, statuses);
    }

    public PeriodRange resolvePeriod(String rawPeriod, Integer requestedYear, Integer requestedMonth,
                                     Integer requestedQuarter, String timezone) {
        return resolvePeriod(rawPeriod, requestedYear, requestedMonth, requestedQuarter, timezone, null);
    }

    public PeriodRange resolvePeriod(String rawPeriod, Integer requestedYear, Integer requestedMonth,
                                     Integer requestedQuarter, String timezone, String date) {
        ZoneId zone;
        try {
            zone = ZoneId.of(timezone == null || timezone.isBlank() ? "Asia/Ho_Chi_Minh" : timezone);
        } catch (DateTimeException exception) {
            throw ApiException.badRequest("Múi giờ không hợp lệ.");
        }
        ZonedDateTime now = ZonedDateTime.now(zone);
        String period = rawPeriod == null ? "YEAR" : rawPeriod.trim().toUpperCase(Locale.ROOT);
        int year = requestedYear == null ? now.getYear() : requestedYear;
        if (year < 2000 || year > 2100) throw ApiException.badRequest("Năm phải nằm trong khoảng 2000–2100.");

        LocalDate start;
        Integer month = null;
        Integer quarter = null;
        switch (period) {
            case "DAY" -> {
                try {
                    start = LocalDate.parse(date == null ? "" : date);
                } catch (DateTimeException exception) {
                    throw ApiException.badRequest("Chọn ngày hợp lệ theo định dạng yyyy-MM-dd.");
                }
                year = start.getYear();
                if (year < 2000 || year > 2100) throw ApiException.badRequest("Năm phải nằm trong khoảng 2000–2100.");
                month = start.getMonthValue();
            }
            case "MONTH" -> {
                month = requestedMonth == null ? now.getMonthValue() : requestedMonth;
                if (month < 1 || month > 12) throw ApiException.badRequest("Tháng phải nằm trong khoảng 1–12.");
                start = LocalDate.of(year, month, 1);
            }
            case "QUARTER" -> {
                quarter = requestedQuarter == null ? ((now.getMonthValue() - 1) / 3) + 1 : requestedQuarter;
                if (quarter < 1 || quarter > 4) throw ApiException.badRequest("Quý phải nằm trong khoảng 1–4.");
                start = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
            }
            case "YEAR" -> start = LocalDate.of(year, 1, 1);
            default -> throw ApiException.badRequest("Kỳ thống kê chỉ hỗ trợ DAY, MONTH, QUARTER hoặc YEAR.");
        }

        LocalDate end = switch (period) {
            case "DAY" -> start.plusDays(1);
            case "MONTH" -> start.plusMonths(1);
            case "QUARTER" -> start.plusMonths(3);
            default -> start.plusYears(1);
        };
        return new PeriodRange(period, year, month, quarter, zone,
                start.atStartOfDay(zone).toInstant(), end.atStartOfDay(zone).toInstant());
    }

    private static Map<String, MutableRevenue> emptyTrend(PeriodRange range) {
        Map<String, MutableRevenue> trend = new LinkedHashMap<>();
        if ("DAY".equals(range.period())) {
            // Iterate actual hours to handle days with 23 or 25 hours at DST boundaries.
            for (Instant hour = range.from(); hour.isBefore(range.to()); hour = hour.plusSeconds(3600)) {
                trend.put(hour.toString(), new MutableRevenue());
            }
        } else if ("MONTH".equals(range.period())) {
            YearMonth selected = YearMonth.of(range.year(), range.month());
            for (int day = 1; day <= selected.lengthOfMonth(); day++) {
                trend.put(selected.atDay(day).toString(), new MutableRevenue());
            }
        } else {
            int firstMonth = "QUARTER".equals(range.period()) ? (range.quarter() - 1) * 3 + 1 : 1;
            int monthCount = "QUARTER".equals(range.period()) ? 3 : 12;
            for (int offset = 0; offset < monthCount; offset++) {
                trend.put(YearMonth.of(range.year(), firstMonth + offset).toString(), new MutableRevenue());
            }
        }
        return trend;
    }

    private static String bucketKey(Instant timestamp, PeriodRange range) {
        if ("DAY".equals(range.period())) {
            long offset = java.time.Duration.between(range.from(), timestamp).toHours();
            return range.from().plusSeconds(offset * 3600).toString();
        }
        ZonedDateTime local = timestamp.atZone(range.zone());
        return "MONTH".equals(range.period()) ? local.toLocalDate().toString() : YearMonth.from(local).toString();
    }

    private static String bucketLabel(String key, PeriodRange range) {
        if ("DAY".equals(range.period())) {
            return Instant.parse(key).atZone(range.zone()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        }
        if ("MONTH".equals(range.period())) {
            LocalDate date = LocalDate.parse(key);
            return "%02d/%02d".formatted(date.getDayOfMonth(), date.getMonthValue());
        }
        return "Thg " + YearMonth.parse(key).getMonthValue();
    }

    private static String normalizeStatus(String rawStatus) {
        String status = rawStatus == null ? "" : rawStatus.toUpperCase(Locale.ROOT);
        if (PAID.equals(status)) return PAID;
        if (PENDING.equals(status)) return PENDING;
        if (REVIEW.equals(status)) return REVIEW;
        return FAILED;
    }

    private static final class MutableRevenue {
        private long revenue;
        private long transactions;

        private void add(long amount) {
            revenue = Math.addExact(revenue, amount);
            transactions++;
        }
    }
}
