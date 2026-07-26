package com.example.inventory.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
public class Supplier extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "supplier_street")),
            @AttributeOverride(name = "city", column = @Column(name = "supplier_city")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "supplier_zip_code")),
            @AttributeOverride(name = "country", column = @Column(name = "supplier_country"))
    })
    private Address address;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "supplier_products",
            joinColumns = @JoinColumn(name = "supplier_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private List<Product> products = new ArrayList<>();

    // Helper methods for ManyToMany sync
    public void addProduct(Product product) {
        products.add(product);
        product.getSuppliers().add(this);
    }

    public void removeProduct(Product product) {
        products.remove(product);
        product.getSuppliers().remove(this);
    }
}
