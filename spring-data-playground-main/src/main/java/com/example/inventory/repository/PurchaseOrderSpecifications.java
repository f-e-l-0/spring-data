package com.example.inventory.repository;

import com.example.inventory.domain.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class PurchaseOrderSpecifications {

    public static Specification<PurchaseOrder> hasStatus(OrderStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<PurchaseOrder> createdAfter(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) ->
                dateTime == null ? criteriaBuilder.conjunction() : criteriaBuilder.greaterThanOrEqualTo(root.get("createdDate"), dateTime);
    }

    public static Specification<PurchaseOrder> hasProductSku(String sku) {
        return (root, query, criteriaBuilder) -> {
            if (sku == null || sku.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            // Join PurchaseOrder -> PurchaseOrderItem -> Product
            Join<PurchaseOrder, PurchaseOrderItem> itemsJoin = root.join("items", JoinType.INNER);
            Join<PurchaseOrderItem, Product> productJoin = itemsJoin.join("product", JoinType.INNER);
            return criteriaBuilder.equal(productJoin.get("sku"), sku);
        };
    }
}
