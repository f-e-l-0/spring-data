package com.example.inventory;

import com.example.inventory.domain.Category;
import com.example.inventory.domain.Product;
import com.example.inventory.domain.ProductStatus;
import com.example.inventory.repository.CategoryRepository;
import com.example.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SoftDeleteAndAuditingTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testAuditingAndSoftDelete() {
        // 1. Test JPA Auditing
        Category category = new Category();
        category.setName("Electronics");
        categoryRepository.save(category);

        Product product = new Product();
        product.setSku("PROD-001");
        product.setName("Wireless Mouse");
        product.setPrice(new BigDecimal("29.99"));
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);

        Product savedProduct = productRepository.save(product);

        // Force a flush to DB to trigger listeners
        productRepository.flush();

        // Assert auditing fields are set
        assertThat(savedProduct.getCreatedDate()).isNotNull();
        assertThat(savedProduct.getCreatedBy()).isEqualTo("system-user");
        assertThat(savedProduct.getLastModifiedDate()).isNotNull();
        assertThat(savedProduct.getLastModifiedBy()).isEqualTo("system-user");

        Long id = savedProduct.getId();

        // 2. Test Soft Delete
        productRepository.delete(savedProduct);
        productRepository.flush();

        // Standard lookup should fail because @SQLRestriction("deleted = false") filters it out
        Optional<Product> lookup = productRepository.findById(id);
        assertThat(lookup).isEmpty();

        List<Product> allProducts = productRepository.findAll();
        assertThat(allProducts).noneMatch(p -> p.getId().equals(id));

        // Use direct JDBC to verify that the row STILL exists in the DB, but with deleted = true
        Boolean dbDeleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM products WHERE id = ?",
                Boolean.class,
                id
        );
        assertThat(dbDeleted).isTrue();
    }
}
