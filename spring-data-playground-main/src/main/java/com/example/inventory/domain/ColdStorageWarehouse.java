package com.example.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cold_storage_warehouses")
@PrimaryKeyJoinColumn(name = "warehouse_id")
@Getter
@Setter
public class ColdStorageWarehouse extends Warehouse {

    @Column(name = "min_temperature", nullable = false)
    private Double minTemperature;
}
