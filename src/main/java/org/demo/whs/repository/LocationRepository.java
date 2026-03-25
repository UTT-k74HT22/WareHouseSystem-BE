package org.demo.whs.repository;

import org.demo.whs.entity.Locations;
import org.demo.whs.entity.enums.LocationStatus;
import org.demo.whs.entity.enums.LocationType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Location entity.
 */
@Repository
public interface LocationRepository extends JpaRepository<Locations, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Locations l WHERE l.id = :id")
    Optional<Locations> findByIdForUpdate(@Param("id") String id);

    /**
     * Checks if a location code exists within a specific warehouse.
     *
     * @param warehouseId the warehouse ID
     * @param code        the location code
     * @return true if exists, false otherwise
     */
    boolean existsByWarehouseIdAndCode(String warehouseId, String code);

    /**
     * Finds a location by warehouse ID.
     *
     * @param warehouseId the warehouse ID
     * @return Optional containing the location if found
     */
    @Query("SELECT l FROM Locations l WHERE l.warehouseId = :warehouseId")
    Optional<Locations> findByWarehouseId(String warehouseId);

    /**
     * Checks if a location code exists within a specific warehouse, excluding a specific location ID.
     * Used for update validation.
     *
     * @param warehouseId the warehouse ID
     * @param code        the location code
     * @param id          the location ID to exclude
     * @return true if exists, false otherwise
     */
    boolean existsByWarehouseIdAndCodeAndIdNot(String warehouseId, String code, String id);

    /**
     * Finds a location by warehouse ID and code.
     *
     * @param warehouseId the warehouse ID
     * @param code        the location code
     * @return Optional containing the location if found
     */
    Optional<Locations> findByWarehouseIdAndCode(String warehouseId, String code);

    /**
     * Finds all locations for a specific warehouse with pagination.
     *
     * @param warehouseId the warehouse ID
     * @param pageable    pagination information
     * @return page of locations
     */
    Page<Locations> findByWarehouseId(String warehouseId, Pageable pageable);

    /**
     * Advanced search with multiple filters.
     *
     * @param warehouseId the warehouse ID (optional)
     * @param code        the location code (optional, partial match)
     * @param name        the location name (optional, partial match)
     * @param zone        the zone (optional, partial match)
     * @param type        the location type (optional)
     * @param status      the location status (optional)
     * @param pageable    pagination information
     * @return page of locations
     */
    @Query("SELECT l FROM Locations l WHERE " +
           "(:warehouseId IS NULL OR l.warehouseId = :warehouseId) AND " +
           "(:code IS NULL OR LOWER(l.code) LIKE LOWER(CONCAT('%', :code, '%'))) AND " +
           "(:name IS NULL OR LOWER(l.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:zone IS NULL OR LOWER(l.zone) LIKE LOWER(CONCAT('%', :zone, '%'))) AND " +
           "(:type IS NULL OR l.type = :type) AND " +
           "(:status IS NULL OR l.status = :status)")
    Page<Locations> searchLocations(
        @Param("warehouseId") String warehouseId,
        @Param("code") String code,
        @Param("name") String name,
        @Param("zone") String zone,
        @Param("type") LocationType type,
        @Param("status") LocationStatus status,
        Pageable pageable
    );

    /**
     * Counts locations by warehouse ID with multiple statuses.
     * Used to validate warehouse deletion.
     *
     * @param warehouseId the warehouse ID
     * @param statuses list of blocking statuses
     * @return count of locations
     */
    long countByWarehouseIdAndStatusNot(String warehouseId, LocationStatus statuses);

    /**
     * Counts all locations for a specific warehouse.
     *
     * @param warehouseId the warehouse ID
     * @return count of locations
     */
    long countByWarehouseId(String warehouseId);

    /**
     * Finds active locations by warehouse and type.
     *
     * @param warehouseId the warehouse ID
     * @param type        the location type
     * @param status      the location status
     * @return list of matching locations
     */
    List<Locations> findByWarehouseIdAndTypeAndStatus(String warehouseId, LocationType type, LocationStatus status);

    /**
     * Atomically increases used capacity with capacity validation.
     * Only updates if: location exists AND used + quantity <= capacity
     *
     * @param locationId the location ID
     * @param quantity   the quantity to add
     * @return number of rows updated (0 if failed)
     */
    @Modifying
    @Query("""
        UPDATE Locations l
        SET l.usedCapacity = l.usedCapacity + :quantity
        WHERE l.id = :locationId
          AND l.usedCapacity + :quantity <= l.capacity
          AND l.status = 'ACTIVE'
        """)
    int increaseUsedCapacity(@Param("locationId") String locationId,
                             @Param("quantity") BigDecimal quantity);

    /**
     * Atomically decreases used capacity with validation.
     * Only updates if: location exists AND used >= quantity
     *
     * @param locationId the location ID
     * @param quantity   the quantity to subtract
     * @return number of rows updated (0 if failed)
     */
    @Modifying
    @Query("""
        UPDATE Locations l
        SET l.usedCapacity = l.usedCapacity - :quantity
        WHERE l.id = :locationId
          AND l.usedCapacity >= :quantity
          AND l.status = 'ACTIVE'
        """)
    int decreaseUsedCapacity(@Param("locationId") String locationId,
                             @Param("quantity") BigDecimal quantity);
}
