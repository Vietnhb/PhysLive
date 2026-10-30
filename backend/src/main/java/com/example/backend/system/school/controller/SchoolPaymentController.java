package com.example.backend.system.school.controller;

import com.example.backend.system.school.dto.SchoolPaymentContracts.SchoolPaymentPlanChoiceRequest;
import com.example.backend.system.school.dto.SchoolPaymentContracts.SchoolPaymentRecoveryRequest;
import com.example.backend.system.school.dto.SchoolPaymentContracts.SchoolRegistrationRequest;
import com.example.backend.system.school.dto.SchoolPaymentContracts;
import com.example.backend.system.school.service.SchoolPaymentService;
import com.example.backend.system.school.service.PaymentAnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SchoolPaymentController {
    private final SchoolPaymentService payments;
    private final PaymentAnalyticsService paymentAnalytics;

    @PostMapping("/api/auth/school-checkout")
    public SchoolPaymentContracts.Checkout checkout(@Valid @RequestBody SchoolRegistrationRequest request,
            HttpServletRequest http) {
        return payments.checkout(request, http.getRemoteAddr());
    }

    @GetMapping("/api/auth/payments/vnpay/ipn")
    public Map<String, String> ipn(@RequestParam Map<String, String> fields) {
        return payments.ipn(fields);
    }

    @PostMapping("/api/auth/school-checkout/recover")
    public SchoolPaymentContracts.Checkout recover(@Valid @RequestBody SchoolPaymentRecoveryRequest credentials, HttpServletRequest http) {
        return payments.recover(credentials, http.getRemoteAddr());
    }

    @GetMapping("/api/school/billing")
    public SchoolPaymentContracts.Billing billing() { return payments.billing(); }
    @PostMapping("/api/school/billing/quote")
    public SchoolPaymentContracts.Quote quote(@Valid @RequestBody SchoolPaymentPlanChoiceRequest request) { return payments.quote(request.planCode()); }
    @PostMapping("/api/school/billing/checkout")
    public SchoolPaymentContracts.Checkout purchase(@Valid @RequestBody SchoolPaymentPlanChoiceRequest request, HttpServletRequest http) { return payments.purchase(request.planCode(), request.expectedAmountVnd(), http.getRemoteAddr()); }
    @GetMapping("/api/auth/payments/{id}")
    public Map<String, String> status(@PathVariable String id) {
        return Map.of("status", payments.status(id));
    }

    @GetMapping("/api/admin/payment-notifications")
    public List<SchoolPaymentContracts.Notification> notifications() {
        return payments.notifications();
    }

    @GetMapping("/api/admin/payments")
    public List<SchoolPaymentContracts.AdminPaymentRow> adminPayments(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) String date) {
        if (period == null) return payments.adminPayments();
        var range = paymentAnalytics.resolvePeriod(period, year, month, quarter, timezone, date);
        return payments.adminPayments(range.from(), range.to());
    }

    @GetMapping("/api/admin/payment-report")
    public SchoolPaymentContracts.RevenueSummary paymentReport() { return payments.revenueSummary(); }

    @GetMapping("/api/admin/payment-analytics")
    public SchoolPaymentContracts.PaymentAnalytics paymentAnalytics(
            @RequestParam(defaultValue = "YEAR") String period,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) String timezone,
            @RequestParam(required = false) String date) {
        return paymentAnalytics.analytics(period, year, month, quarter, timezone, date);
    }

    @PostMapping("/api/admin/payments/{id}/reconcile")
    public Map<String, String> reconcile(@PathVariable UUID id) { return Map.of("status", payments.reconcilePayment(id)); }
}
