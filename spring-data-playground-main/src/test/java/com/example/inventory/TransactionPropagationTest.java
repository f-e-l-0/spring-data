package com.example.inventory;

import com.example.inventory.domain.OrderStatus;
import com.example.inventory.domain.PurchaseOrder;
import com.example.inventory.exception.OrderProcessingException;
import com.example.inventory.repository.PurchaseOrderRepository;
import com.example.inventory.service.OrderProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class TransactionPropagationTest {

    @Autowired
    private OrderProcessingService orderProcessingService;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @BeforeEach
    void setUp() {
        purchaseOrderRepository.deleteAllInBatch();
    }

    @Test
    void testRuntimeRollback() {
        PurchaseOrder order = new PurchaseOrder();
        order.setStatus(OrderStatus.DELIVERED);

        assertThrows(RuntimeException.class, () -> {
            orderProcessingService.createOrderWithRuntimeRollback(order);
        });

        // The transaction should have rolled back, so the order is not in the database
        assertThat(purchaseOrderRepository.count()).isEqualTo(0);
    }

    @Test
    void testCheckedExceptionNoRollback() throws OrderProcessingException {
        PurchaseOrder order = new PurchaseOrder();
        order.setStatus(OrderStatus.SHIPPED);

        // Checked exception doesn't trigger rollback by default
        assertThrows(OrderProcessingException.class, () -> {
            orderProcessingService.createOrderWithCheckedNoRollback(order);
        });

        // The transaction committed successfully, order is saved
        assertThat(purchaseOrderRepository.count()).isEqualTo(1);
    }

    @Test
    void testCheckedExceptionWithRollbackFor() {
        PurchaseOrder order = new PurchaseOrder();
        order.setStatus(OrderStatus.SHIPPED);

        assertThrows(OrderProcessingException.class, () -> {
            orderProcessingService.createOrderWithCheckedRollback(order);
        });

        // Configured with rollbackFor, so the transaction rolled back
        assertThat(purchaseOrderRepository.count()).isEqualTo(0);
    }

    @Test
    void testRequiresNewPropagation() {
        PurchaseOrder mainOrder = new PurchaseOrder();
        mainOrder.setStatus(OrderStatus.DELIVERED);

        // This method saves mainOrder, calls AuditLogService.logAttempt() (REQUIRES_NEW),
        // and then throws a RuntimeException to trigger rollback of the outer transaction.
        assertThrows(RuntimeException.class, () -> {
            orderProcessingService.processOrderWithRequiresNewLog(mainOrder, "Main Order Log Info");
        });

        // The main transaction rolled back, so the DELIVERED mainOrder is NOT saved.
        // However, the REQUIRES_NEW log transaction committed, so the log order IS saved!
        // We find the log because its status is default PENDING in AuditLogService
        long totalOrders = purchaseOrderRepository.count();
        assertThat(totalOrders).isEqualTo(1);

        PurchaseOrder savedLog = purchaseOrderRepository.findAll().getFirst();
        assertThat(savedLog.getStatus()).isEqualTo(OrderStatus.PENDING);
    }
}
