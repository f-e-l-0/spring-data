package com.example.inventory.service;

import com.example.inventory.domain.OrderStatus;
import com.example.inventory.domain.PurchaseOrder;
import com.example.inventory.exception.OrderProcessingException;
import com.example.inventory.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderProcessingService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final AuditLogService auditLogService;

    /**
     * Outer transaction. If this fails and rolls back, any nested transactions
     * marked with Propagation.REQUIRES_NEW will STILL commit.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void processOrderWithRequiresNewLog(PurchaseOrder order, String logContent) {
        // Save the main order
        purchaseOrderRepository.save(order);

        // Call the independent REQUIRES_NEW logging service method
        auditLogService.logAttempt(logContent);

        // Throw an exception to force rollback of the outer transaction
        throw new RuntimeException("Simulated error in outer transaction to trigger rollback");
    }

    /**
     * Spring transactions automatically rollback on RUNTIME (Unchecked) exceptions.
     */
    @Transactional
    public void createOrderWithRuntimeRollback(PurchaseOrder order) {
        purchaseOrderRepository.save(order);
        throw new RuntimeException("Simulated runtime error to trigger rollback");
    }

    /**
     * Spring transactions do NOT rollback on CHECKED exceptions by default.
     * The transaction will commit, and the order will be saved!
     */
    @Transactional
    public void createOrderWithCheckedNoRollback(PurchaseOrder order) throws OrderProcessingException {
        purchaseOrderRepository.save(order);
        throw new OrderProcessingException("Simulated checked exception (No rollback by default)");
    }

    /**
     * Force rollback for checked exceptions by using the 'rollbackFor' attribute.
     */
    @Transactional(rollbackFor = OrderProcessingException.class)
    public void createOrderWithCheckedRollback(PurchaseOrder order) throws OrderProcessingException {
        purchaseOrderRepository.save(order);
        throw new OrderProcessingException("Simulated checked exception with rollbackFor configured");
    }
}
