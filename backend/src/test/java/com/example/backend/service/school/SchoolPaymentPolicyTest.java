package com.example.backend.service.school;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.backend.config.properties.VnpayProperties;
import com.example.backend.entity.User;
import com.example.backend.entity.school.*;
import com.example.backend.repository.*;
import com.example.backend.repository.school.*;
import com.example.backend.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import java.net.http.HttpClient;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class SchoolPaymentPolicyTest {
    private final SchoolPaymentRepository payments = mock(SchoolPaymentRepository.class);
    private final SchoolRepository schools = mock(SchoolRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final SchoolPaymentService service = new SchoolPaymentService(mock(LicensePlanRepository.class), payments,
            schools, users, mock(RoleRepository.class), mock(PasswordEncoder.class), mock(CurrentUserService.class),
            new LicenseCheckService(), mock(EntityManager.class),
            new VnpayProperties("TEST", "test-signing-secret", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                    "http://localhost/result", "", "127.0.0.1", null, null, null), mock(HttpClient.class));
    private SchoolPayment payment;
    private School school;

    @BeforeEach void setup() {
        school = new School(); school.setId(UUID.randomUUID()); school.setActive(false);
        User manager = new User(); manager.setActive(false); manager.setEnabled(false);
        payment = new SchoolPayment(); payment.setId(UUID.randomUUID()); payment.setSchool(school); payment.setManager(manager);
        payment.setPlanCode("BASIC"); payment.setAmountVnd(100000L); payment.setStudentQuota(50); payment.setMonthlyTokenQuota(1000);
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(payments.findLockedById(payment.getId())).thenReturn(Optional.of(payment));
        when(schools.findByIdForUpdate(school.getId())).thenReturn(Optional.of(school));
    }
    private Map<String,String> signed(long amount) {
        Map<String,String> fields = new HashMap<>();
        fields.put("vnp_TmnCode", "TEST"); fields.put("vnp_TxnRef", payment.getId().toString());
        fields.put("vnp_Amount", Long.toString(amount)); fields.put("vnp_ResponseCode", "00");
        fields.put("vnp_TransactionStatus", "00"); fields.put("vnp_TransactionNo", "123");
        fields.put("vnp_SecureHash", service.sign(SchoolPaymentService.canonical(fields))); return fields;
    }
    @Test void invalidSignatureCannotActivateSchool() {
        var fields = signed(10000000); fields.put("vnp_Amount", "1");
        assertThat(service.ipn(fields).get("RspCode")).isEqualTo("97");
        assertThat(school.isActive()).isFalse(); verify(payments, never()).findLockedById(any());
    }
    @Test void evenSignedWrongAmountCannotActivateSchool() {
        assertThat(service.ipn(signed(1)).get("RspCode")).isEqualTo("04");
        assertThat(school.isActive()).isFalse();
    }
    @Test void verifiedPaymentActivatesOnce() {
        assertThat(service.ipn(signed(10000000)).get("RspCode")).isEqualTo("00");
        assertThat(school.isActive()).isTrue(); assertThat(payment.getManager().isEnabled()).isTrue();
        LocalDate end = school.getLicenseEnd();
        assertThat(service.ipn(signed(10000000)).get("RspCode")).isEqualTo("02");
        assertThat(school.getLicenseEnd()).isEqualTo(end); assertThat(payment.getStatus()).isEqualTo("PAID");
    }
    @Test void browserStatusReadDoesNotActivatePendingSchool() {
        assertThat(service.status(payment.getId())).isEqualTo("PENDING");
        assertThat(school.isActive()).isFalse();
    }
}
