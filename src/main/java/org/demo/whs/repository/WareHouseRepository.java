package org.demo.whs.repository;

import org.demo.whs.entity.Locations;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.response.WareHouse.WareHouseResponse;
import org.demo.whs.entity.enums.WareHouseStatus;
import org.demo.whs.entity.enums.WareHouseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Warehouses entity.
 */
@Repository
public interface WareHouseRepository extends JpaRepository<Warehouses, String> {

    /**
     * Checks if a warehouse exists by its code.
     *
     * @param code the warehouse code
     * @return true if a warehouse with the given code exists, false otherwise
     */
    boolean existsByCode(String code);

    long countByStatus(WareHouseStatus status);

    /**
     * Checks if an active warehouse exists by its ID.
     *
     * @param id the warehouse ID
     * @return true if an active warehouse with the given ID exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM Warehouses w WHERE w.id = :id AND w.status = 'ACTIVE'")
    boolean existsActiveById(String id);

    /**
     * Finds warehouses by a collection of IDs.
     *
     * @param ids the collection of warehouse IDs
     * @return list of warehouses matching the given IDs
     */
    List<Warehouses> findByIdIn(Collection<String> ids);

    @Query("SELECT w FROM Warehouses w WHERE "
            + "(:keyword IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(w.code) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(w.address) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
            + "AND (:status IS NULL OR w.status = :status) "
            + "AND (:type IS NULL OR w.type = :type)")
    Page<Warehouses> searchWarehouses(
            @Param("keyword") String keyword,
            @Param("status") WareHouseStatus status,
            @Param("type") WareHouseType type,
            Pageable pageable);
}
