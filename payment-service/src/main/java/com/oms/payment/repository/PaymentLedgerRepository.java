package com.oms.payment.repository;

import com.oms.payment.domain.PaymentLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentLedgerRepository extends JpaRepository<PaymentLedger, UUID> {
    List<PaymentLedger> findByOrderId(UUID orderId);
}