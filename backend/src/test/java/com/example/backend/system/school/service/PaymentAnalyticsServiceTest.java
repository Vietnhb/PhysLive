package com.example.backend.system.school.service;

import com.example.backend.exception.ApiException;
import com.example.backend.system.school.repository.SchoolPaymentRepository;
import com.example.backend.system.school.repository.SchoolPaymentRepository.PaymentAnalyticsRow;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentAnalyticsServiceTest {
    private final SchoolPaymentRepository payments = mock(SchoolPaymentRepository.class);
    private final PaymentAnalyticsService service = new PaymentAnalyticsService(payments);

    @Test
    void yearlyAnalyticsCountsOnlyPaidRevenueAndUsesPaidTimestampBuckets() {
        List<PaymentAnalyticsRow> rows = List.of(
                row("STARTER", 30_000_000L, "PAID", "2026-01-01T01:00:00Z", "2026-02-10T02:00:00Z"),
                row("STARTER", 20_000_000L, "PAID", "2026-02-01T01:00:00Z", "2026-02-11T02:00:00Z"),
                row("CUSTOM", 40_000_000L, "PAID", "2026-02-01T01:00:00Z", "2026-03-05T02:00:00Z"),
                row("LEGACY", 10_000_000L, "PAID", "2026-04-05T02:00:00Z", null),
                row("STARTER", 99_000_000L, "PENDING", "2026-04-01T01:00:00Z", null),
                row("STARTER", 99_000_000L, "REQUIRES_REVIEW", "2026-05-01T01:00:00Z", "2026-05-01T02:00:00Z"),
                row("STARTER", 99_000_000L, "FAILED", "2026-06-01T01:00:00Z", null));
        when(payments.findAnalyticsRows(any(), any())).thenReturn(rows);

        var result = service.analytics("YEAR", 2026, null, null, "Asia/Ho_Chi_Minh");

        assertEquals(100_000_000L, result.kpis().grossPaidVnd());
        assertEquals(4, result.kpis().paidTransactions());
        assertEquals(1, result.kpis().pendingTransactions());
        assertEquals(1, result.kpis().reviewTransactions());
        assertEquals(1, result.kpis().failedTransactions());
        assertEquals(7, result.kpis().totalTransactions());
        assertEquals(12, result.trend().size());
        assertEquals(50_000_000L, result.trend().get(1).revenueVnd());
        assertEquals(40_000_000L, result.trend().get(2).revenueVnd());
        assertEquals(10_000_000L, result.trend().get(3).revenueVnd(), "Legacy PAID rows fall back to createdAt when paidAt is absent");
        assertEquals("STARTER", result.packages().get(0).planCode());
        assertEquals(50_000_000L, result.packages().get(0).revenueVnd());
    }

    @Test
    void periodUsesHalfOpenBoundariesInRequestedTimezone() {
        when(payments.findAnalyticsRows(any(), any())).thenReturn(List.of());

        var result = service.analytics("MONTH", 2026, 10, null, "Asia/Ho_Chi_Minh");

        assertEquals(Instant.parse("2026-09-30T17:00:00Z"), result.from());
        assertEquals(Instant.parse("2026-10-31T17:00:00Z"), result.to());
        assertEquals(31, result.trend().size());
        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        verify(payments).findAnalyticsRows(from.capture(), to.capture());
        assertEquals(result.from(), from.getValue());
        assertEquals(result.to(), to.getValue());
    }

    @Test
    void rejectsInvalidPeriodInputsBeforeQueryingDatabase() {
        assertThrows(ApiException.class, () -> service.analytics("MONTH", 2026, 13, null, "Asia/Ho_Chi_Minh"));
        assertThrows(ApiException.class, () -> service.analytics("QUARTER", 2026, null, 5, "Asia/Ho_Chi_Minh"));
        assertThrows(ApiException.class, () -> service.analytics("WEEK", 2026, null, null, "Asia/Ho_Chi_Minh"));
        assertThrows(ApiException.class, () -> service.analytics("YEAR", 2026, null, null, "Not/A_Zone"));
        verifyNoInteractions(payments);
    }

    private PaymentAnalyticsRow row(String plan, Long amount, String status, String createdAt, String paidAt) {
        PaymentAnalyticsRow row = mock(PaymentAnalyticsRow.class);
        when(row.getPlanCode()).thenReturn(plan);
        when(row.getAmountVnd()).thenReturn(amount);
        when(row.getStatus()).thenReturn(status);
        when(row.getCreatedAt()).thenReturn(Instant.parse(createdAt));
        when(row.getPaidAt()).thenReturn(paidAt == null ? null : Instant.parse(paidAt));
        return row;
    }
}
