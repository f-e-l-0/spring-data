package com.example.inventory;

import com.example.inventory.domain.Product;
import com.example.inventory.domain.ProductStatus;
import com.example.inventory.repository.ProductRepository;
import com.example.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class LockingIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryService inventoryService;

    private Long productId;

    @BeforeEach
    void setUp() {
        productRepository.deleteAllInBatch();
        Product product = new Product();
        product.setSku("LOCK-001");
        product.setName("Secure Item");
        product.setPrice(new BigDecimal("100.00"));
        product.setStatus(ProductStatus.ACTIVE);
        product = productRepository.save(product);
        productId = product.getId();
    }

    @Test
    void testOptimisticLocking() {
        // Fetch the same database row into two separate instances
        Product instanceA = productRepository.findById(productId).orElseThrow();
        Product instanceB = productRepository.findById(productId).orElseThrow();

        assertThat(instanceA.getVersion()).isEqualTo(0);
        assertThat(instanceB.getVersion()).isEqualTo(0);

        // Transaction A updates and commits first
        instanceA.setPrice(new BigDecimal("120.00"));
        productRepository.saveAndFlush(instanceA);

        // Assert the version was incremented to 1
        Product checkVersion = productRepository.findById(productId).orElseThrow();
        assertThat(checkVersion.getVersion()).isEqualTo(1);

        // Transaction B attempts to update the row with the old version (0)
        instanceB.setPrice(new BigDecimal("150.00"));

        // Saving instanceB must fail because its version (0) is less than the current DB version (1)
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            productRepository.saveAndFlush(instanceB);
        });
    }

    @Test
    void testPessimisticLocking() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Task 1: Acquires a Pessimistic Write Lock and sleeps for 1 second inside transaction
        Future<Long> task1 = executor.submit(() -> {
            long startTime = System.currentTimeMillis();
            inventoryService.updateProductPricePessimistically(productId, new BigDecimal("200.00"), 1000);
            return System.currentTimeMillis() - startTime;
        });

        // Small delay to ensure Task 1 starts first and acquires the lock
        Thread.sleep(200);

        // Task 2: Attempts to acquire lock/update the same row immediately.
        // Because of the pessimistic lock, Task 2 will block until Task 1 commits.
        Future<Long> task2 = executor.submit(() -> {
            long startTime = System.currentTimeMillis();
            inventoryService.updateProductPricePessimistically(productId, new BigDecimal("300.00"), 0);
            return System.currentTimeMillis() - startTime;
        });

        task1.get();
        Long task2Duration = task2.get();

        // Task 2 must have waited for Task 1's sleep of 1000ms to complete before execution.
        // Therefore, task2's duration should be at least 800ms due to blocking on the lock.
        assertThat(task2Duration).isGreaterThanOrEqualTo(800);

        // Verify the final price is Task 2's price (300.00)
        Product finalProduct = productRepository.findById(productId).orElseThrow();
        assertThat(finalProduct.getPrice()).isEqualByComparingTo("300.00");

        executor.shutdown();
    }
}
