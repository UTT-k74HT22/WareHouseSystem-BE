package org.demo.whs.entity.enums;

/**
 * Status for inventory reservations.
 */
public enum InventoryReservationStatus {
    RESERVED,  // Stock is held
    RELEASED,  // Reservation cancelled, stock returned to available
    CONSUMED   // Stock picked and shipped, reservation completed
}
