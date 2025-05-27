package com.app.ch1dbindexing.repository;

import com.app.ch1dbindexing.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByProductName(String productName);

    Optional<Product> findBySerialNumber(String serialNumber);
}
