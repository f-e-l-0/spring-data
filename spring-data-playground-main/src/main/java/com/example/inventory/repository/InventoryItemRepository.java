package com.example.inventory.repository;

import com.example.inventory.domain.InventoryItem;
import com.example.inventory.domain.InventoryItemId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, InventoryItemId> {
}
