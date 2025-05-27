package com.app.ch1dbindexing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_serial_number", columnList = "serialNumber", unique = true)
})
@Getter // Getter 자동 생성
@NoArgsConstructor // 파라미터 없는 기본 생성자 자동 생성
@AllArgsConstructor // 모든 필드를 파라미터로 받는 생성자 자동 생성
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productName; // 인덱스 없음 (비교 대상)

    @Column(nullable = false, unique = true)
    private String serialNumber; // 인덱스 있음

    private String category;

    private double price;

    public static Product create(String productName, String serialNumber, String category, double price) {
        Product product = new Product();
        product.productName = productName;
        product.serialNumber = serialNumber;
        product.category = category;
        product.price = price;

        return product;
    }
}
