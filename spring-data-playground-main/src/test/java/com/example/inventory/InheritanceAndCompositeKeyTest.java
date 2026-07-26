package com.example.inventory;

import com.example.inventory.domain.*;
import com.example.inventory.repository.InventoryItemRepository;
import com.example.inventory.repository.ProductRepository;
import com.example.inventory.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class InheritanceAndCompositeKeyTest {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Test
    void testInheritanceMapping() {
        // Create cold storage warehouse
        ColdStorageWarehouse cold = new ColdStorageWarehouse();
        cold.setName("North Pole Freezer");
        cold.setMinTemperature(-18.5);
        warehouseRepository.save(cold);

        // Create hazmat warehouse
        HazardousMaterialWarehouse haz = new HazardousMaterialWarehouse();
        haz.setName("Toxic Bunker Gamma");
        haz.setSafetyRating("Class A");
        haz.setHazardClass("Flammables");
        warehouseRepository.save(haz);

        warehouseRepository.flush();

        // Query polymorphically
        List<Warehouse> warehouses = warehouseRepository.findAll();
        assertThat(warehouses).hasSize(2);

        // Verify correct types are retrieved
        Optional<Warehouse> coldLookup = warehouseRepository.findByName("North Pole Freezer");
        assertThat(coldLookup).isPresent();
        assertThat(coldLookup.get()).isInstanceOf(ColdStorageWarehouse.class);
        assertThat(((ColdStorageWarehouse) coldLookup.get()).getMinTemperature()).isEqualTo(-18.5);

        Optional<Warehouse> hazLookup = warehouseRepository.findByName("Toxic Bunker Gamma");
        assertThat(hazLookup).isPresent();
        assertThat(hazLookup.get()).isInstanceOf(HazardousMaterialWarehouse.class);
        assertThat(((HazardousMaterialWarehouse) hazLookup.get()).getHazardClass()).isEqualTo("Flammables");
    }

    @Test
    void testCompositePrimaryKey() {
        // Create Product
        Product product = new Product();
        product.setSku("COMP-101");
        product.setName("Widget");
        product.setPrice(new BigDecimal("9.99"));
        product.setStatus(ProductStatus.ACTIVE);
        productRepository.save(product);

        // Create Warehouse
        Warehouse warehouse = new Warehouse();
        warehouse.setName("Central Hub");
        warehouseRepository.save(warehouse);

        productRepository.flush();
        warehouseRepository.flush();

        // Create InventoryItem with Composite Primary Key
        InventoryItem item = new InventoryItem(product, warehouse, 150);
        inventoryItemRepository.save(item);
        inventoryItemRepository.flush();

        // Retrieve and Assert
        InventoryItemId id = new InventoryItemId(product.getId(), warehouse.getId());
        Optional<InventoryItem> lookup = inventoryItemRepository.findById(id);

        assertThat(lookup).isPresent();
        assertThat(lookup.get().getQuantity()).isEqualTo(150);
        assertThat(lookup.get().getProduct().getSku()).isEqualTo("COMP-101");
        assertThat(lookup.get().getWarehouse().getName()).isEqualTo("Central Hub");
    }
}
