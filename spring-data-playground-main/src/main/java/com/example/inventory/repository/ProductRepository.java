package com.example.inventory.repository;

import com.example.inventory.domain.Product;
import com.example.inventory.domain.ProductStatus;
import com.example.inventory.dto.ProductDetailDto;
import com.example.inventory.dto.ProductSummaryDto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 1. Derived query method
    Optional<Product> findBySku(String sku);

    List<Product> findByStatus(ProductStatus status);

    // 2. Pagination and Sorting
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    List<Product> findByPriceLessThan(BigDecimal price);

    // 3. Custom JPQL Query
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice")
    List<Product> findProductsInPriceRange(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    // 4. Custom Native Query
    @Query(value = "SELECT * FROM products WHERE status = :statusText AND deleted = false", nativeQuery = true)
    List<Product> findProductsByStatusNative(@Param("statusText") String statusText);

    // 5. Modifying query for bulk updates (clearAutomatically = true clears the L1 cache)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.price = p.price * :multiplier WHERE p.category.id = :categoryId")
    int updateProductPricesByCategory(@Param("categoryId") Long categoryId, @Param("multiplier") BigDecimal multiplier);

    // 6. Entity Graph to resolve the N+1 Query Problem (loads category eagerly)
    @Override
    @EntityGraph(attributePaths = {"category"})
    List<Product> findAll();

    // 7. Interface-based Projection
    List<ProductSummaryDto> findSummaryByStatus(ProductStatus status);

    // 8. Class-based Projection using Constructor Expression
    @Query("SELECT new com.example.inventory.dto.ProductDetailDto(p.sku, p.name, p.price, c.name) " +
           "FROM Product p JOIN p.category c WHERE p.status = :status")
    List<ProductDetailDto> findDetailsByStatus(@Param("status") ProductStatus status);

    // 9. Dynamic Projection
    <T> Collection<T> findByStatus(ProductStatus status, Class<T> type);

    // 10. Pessimistic Locking
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
