package com.example.inventory.dto;

import java.math.BigDecimal;

public interface ProductSummaryDto {
    String getSku();
    String getName();
    BigDecimal getPrice();
}
