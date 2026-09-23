package com.example.backend.service.school;

import com.example.backend.config.properties.VnpayProperties;
import com.example.backend.repository.school.SchoolPaymentRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Separate transaction per payment: a failed provider query must not roll back other payments. */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationScheduler {
    private final VnpayProperties configuration;
    private final SchoolPaymentRepository payments;
    private final SchoolPaymentService service;

    @Scheduled(fixedDelayString = "${VNPAY_RECONCILE_DELAY_MS:60000}")
    public void reconcileExpiredPayments() {
        if (configuration.tmnCode().isBlank() || configuration.hashSecret().isBlank()) return;
        for (var payment : payments.findByStatusAndCreatedAtBefore("PENDING", Instant.now().minusSeconds(900))) {
            try { service.reconcilePayment(payment.getId()); }
            catch (RuntimeException exception) {
                log.warn("Payment {} remains pending reconciliation", payment.getId(), exception);
            }
        }
    }
}
