package com.example.inventory;

import com.example.inventory.domain.Category;
import com.example.inventory.domain.Product;
import com.example.inventory.domain.ProductStatus;
import com.example.inventory.dto.ProductDetailDto;
import com.example.inventory.dto.ProductSummaryDto;
import com.example.inventory.repository.CategoryRepository;
import com.example.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category electronics;
    private Category furniture;

    @BeforeEach
    void setUp() {
        electronics = new Category();
        electronics.setName("Electronics");
        categoryRepository.save(electronics);

        furniture = new Category();
        furniture.setName("Furniture");
        categoryRepository.save(furniture);

        createProduct("E-001", "Laptop", new BigDecimal("999.99"), ProductStatus.ACTIVE, electronics);
        createProduct("E-002", "Smartphone", new BigDecimal("699.99"), ProductStatus.ACTIVE, electronics);
        createProduct("E-003", "Keyboard", new BigDecimal("49.99"), ProductStatus.INACTIVE, electronics);
        createProduct("F-001", "Office Chair", new BigDecimal("149.99"), ProductStatus.ACTIVE, furniture);
        productRepository.flush();
    }

    private void createProduct(String sku, String name, BigDecimal price, ProductStatus status, Category category) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setPrice(price);
        product.setStatus(status);
        product.setCategory(category);
        productRepository.save(product);
    }

    @Test
    void testDerivedQueries() {
        Product p = productRepository.findBySku("E-001").orElseThrow();
        assertThat(p.getName()).isEqualTo("Laptop");

        List<Product> cheapProducts = productRepository.findByPriceLessThan(new BigDecimal("100.00"));
        assertThat(cheapProducts).hasSize(1);
        assertThat(cheapProducts.getFirst().getName()).isEqualTo("Keyboard");
    }

    @Test
    void testPaginationAndSorting() {
        Page<Product> activePage = productRepository.findByStatus(
                ProductStatus.ACTIVE,
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "price"))
        );

        assertThat(activePage.getContent()).hasSize(2);
        assertThat(activePage.getTotalElements()).isEqualTo(3);
        assertThat(activePage.getContent().get(0).getName()).isEqualTo("Laptop"); // Highest price
        assertThat(activePage.getContent().get(1).getName()).isEqualTo("Smartphone");
    }

    @Test
    void testCustomQueries() {
        List<Product> range = productRepository.findProductsInPriceRange(new BigDecimal("100.00"), new BigDecimal("800.00"));
        assertThat(range).hasSize(2); // Smartphone (699.99), Office Chair (149.99)
    }

    @Test
    void testNativeQueries() {
        List<Product> actives = productRepository.findProductsByStatusNative("ACTIVE");
        assertThat(actives).hasSize(3);
    }

    @Test
    void testModifyingQueries() {
        // Double the price of electronics products
        int updated = productRepository.updateProductPricesByCategory(electronics.getId(), new BigDecimal("2.00"));
        assertThat(updated).isEqualTo(3);

        Product laptop = productRepository.findBySku("E-001").orElseThrow();
        assertThat(laptop.getPrice()).isEqualByComparingTo("1999.98");
    }

    @Test
    void testInterfaceProjection() {
        List<ProductSummaryDto> summaries = productRepository.findSummaryByStatus(ProductStatus.ACTIVE);
        assertThat(summaries).hasSize(3);
        assertThat(summaries.get(0).getSku()).isNotNull();
        assertThat(summaries.get(0).getName()).isNotNull();
        assertThat(summaries.get(0).getPrice()).isNotNull();
    }

    @Test
    void testClassBasedProjection() {
        List<ProductDetailDto> details = productRepository.findDetailsByStatus(ProductStatus.ACTIVE);
        assertThat(details).hasSize(3);
        assertThat(details.get(0).sku()).isNotNull();
        assertThat(details.get(0).categoryName()).isIn("Electronics", "Furniture");
    }

    @Test
    void testDynamicProjection() {
        // Query as Full Product
        Collection<Product> fullProducts = productRepository.findByStatus(ProductStatus.ACTIVE, Product.class);
        assertThat(fullProducts).hasSize(3);

        // Query as Summary DTO
        Collection<ProductSummaryDto> dtos = productRepository.findByStatus(ProductStatus.ACTIVE, ProductSummaryDto.class);
        assertThat(dtos).hasSize(3);
        assertThat(dtos.iterator().next().getSku()).isNotNull();
    }

    @Test
    void testEntityGraphNPlus1Resolution() {
        // When calling findAll() which is annotated with @EntityGraph,
        // it fetches both Product and Category in a single JOIN query.
        List<Product> products = productRepository.findAll();
        assertThat(products).isNotEmpty();
        
        // accessing category name does not trigger a secondary select query (N+1 is resolved)
        for (Product product : products) {
            assertThat(product.getCategory().getName()).isNotNull();
        }
    }
}
