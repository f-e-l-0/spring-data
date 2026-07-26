package com.example.inventory.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "product_metadata")
@Getter
@Setter
public class ProductMetadata {

    @Id
    private Long id;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String description;

    @Column(length = 30)
    private String weight;

    @Column(length = 50)
    private String dimensions;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;
}
