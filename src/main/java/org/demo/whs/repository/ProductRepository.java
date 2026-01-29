package org.demo.whs.repository;

import org.demo.whs.entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Products entity.
 */
@Repository
public interface ProductRepository extends JpaRepository<Products, String> {
}
