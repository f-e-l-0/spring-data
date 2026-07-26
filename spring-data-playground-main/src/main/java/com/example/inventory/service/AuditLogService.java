package com.example.inventory.service;

import com.example.inventory.domain.OrderStatus;
import com.example.inventory.domain.PurchaseOrder;
import com.example.inventory.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final PurchaseOrderRepository purchaseOrderRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PurchaseOrder logAttempt(String details) {
        PurchaseOrder auditRecord = new PurchaseOrder();
        auditRecord.setStatus(OrderStatus.PENDING);
        return purchaseOrderRepository.save(auditRecord);
    }
}
