package com.example.inventory.service;

import com.example.inventory.domain.Product;
import com.example.inventory.domain.ProductStatus;
import com.example.inventory.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;

    @PersistenceContext
    private final EntityManager entityManager;

    /**
     * Demonstrates the 4 JPA Entity States:
     * 1. Transient: instantiated but not associated with a persistence context.
     * 2. Managed: associated with a persistence context; changes automatically flushed.
     * 3. Detached: persistence context closed or entity detached.
     * 4. Removed: scheduled for deletion.
     */
    @Transactional
    public Long demonstrateEntityStates(String sku, String name, BigDecimal price) {
        // 1. TRANSIENT State
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setPrice(price);
        product.setStatus(ProductStatus.ACTIVE);
        
        System.out.println("Is entity managed (Transient)? " + entityManager.contains(product));

        // 2. MANAGED State
        // Saving the entity associates it with the persistence context (Session)
        product = productRepository.save(product);
        System.out.println("Is entity managed (Saved)? " + entityManager.contains(product));

        // Let's modify a property of a managed entity.
        // Hibernate dirty-checking will automatically detect this and write it to the DB on commit!
        product.setName(name + " (Updated in Managed State)");

        // 3. DETACHED State
        // Evicting (detaching) the entity from the session
        entityManager.detach(product);
        System.out.println("Is entity managed (Detached)? " + entityManager.contains(product));

        // Modifying a detached entity. This will NOT be written to the database!
        product.setName("This should never be saved");

        // Merge back to managed state
        Product managedProduct = entityManager.merge(product);
        System.out.println("Is merged entity managed? " + entityManager.contains(managedProduct));
        managedProduct.setName(name + " (Final)");

        return managedProduct.getId();
    }

    /**
     * Demonstrates Optimistic Locking.
     * If two transactions fetch the same entity, modify it, and commit concurrently,
     * the second commit will fail with an OptimisticLockingFailureException because the version mismatch is detected.
     */
    @Transactional
    public void updateProductPriceOptimistically(Long productId, BigDecimal newPrice) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        
        // Simulating some processing time or concurrent read
        product.setPrice(newPrice);
        
        // Hibernate automatically increments the version field on update and checks it in the WHERE clause:
        // UPDATE products SET price = ?, version = version + 1 WHERE id = ? AND version = ?
    }

    /**
     * Demonstrates Pessimistic Locking.
     * Using LockModeType.PESSIMISTIC_WRITE locks the database row immediately upon select.
     * Any other concurrent transaction trying to read for update or write to this row will block until this transaction completes.
     */
    @Transactional
    public void updateProductPricePessimistically(Long productId, BigDecimal newPrice, long sleepMs) {
        // This query locks the row: SELECT ... FOR UPDATE (in supported DBs)
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        product.setPrice(newPrice);

        if (sleepMs > 0) {
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
