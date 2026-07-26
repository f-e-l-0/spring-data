package com.example.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "hazardous_material_warehouses")
@PrimaryKeyJoinColumn(name = "warehouse_id")
@Getter
@Setter
public class HazardousMaterialWarehouse extends Warehouse {

    @Column(name = "safety_rating", nullable = false, length = 10)
    private String safetyRating;

    @Column(name = "hazard_class", nullable = false, length = 50)
    private String hazardClass;
}
