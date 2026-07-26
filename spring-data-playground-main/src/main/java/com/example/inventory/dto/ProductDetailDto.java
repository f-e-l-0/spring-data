package com.example.inventory.dto;

import java.math.BigDecimal;

public record ProductDetailDto(
        String sku,
        String name,
        BigDecimal price,
        String categoryName
) {}
