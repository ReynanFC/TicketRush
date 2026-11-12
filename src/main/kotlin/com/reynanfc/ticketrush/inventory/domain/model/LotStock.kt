package com.reynanfc.ticketrush.inventory.domain.model

import com.reynanfc.ticketrush.inventory.domain.exception.InsufficientStockException
import com.reynanfc.ticketrush.inventory.domain.exception.InvalidReservationTransitionException
import java.time.Instant
import java.util.UUID

// Aggregate root of the inventory: how many tickets of one lot are still for sale.
// Reservations are not kept in a list here: a lot can have thousands of them and reserving
// only needs the counter. Each Reservation points back to its lot by lotId.
class LotStock(
    val lotId: UUID,
    val sectorId: UUID,
    val total: Int,
) {
    var available: Int = total
        private set

    init {
        require(total > 0) { "Total must be positive" }
    }

    companion object {
        // Rebuilds a stock that already exists in storage, keeping how many are left.
        fun restore(
            lotId: UUID,
            sectorId: UUID,
            total: Int,
            available: Int,
        ): LotStock {
            require(available in 0..total) { "Available must be between 0 and $total, but was $available" }

            return LotStock(lotId = lotId, sectorId = sectorId, total = total).apply {
                this.available = available
            }
        }
    }

    /**
     * Takes [quantity] tickets out of the stock and returns the pending reservation that holds them.
     *
     * This only protects one object in memory. Two requests that each load their own copy from the
     * database can both pass the check; stopping that is the job of persistence and locking (#14-#16).
     *
     * @throws InsufficientStockException if there are fewer than [quantity] tickets left
     */
    fun reserve(orderId: UUID, quantity: Int, expiresAt: Instant): Reservation {
        require(quantity > 0) { "Quantity must be positive" }

        if (quantity > available) {
            throw InsufficientStockException(lotId, quantity, available)
        }

        available -= quantity

        return Reservation(
            orderId = orderId,
            lotId = lotId,
            quantity = quantity,
            expiresAt = expiresAt,
        )
    }

    /**
     * Gives the tickets of a pending reservation back to the stock.
     * Releasing the same reservation twice gives them back only once.
     *
     * @throws InvalidReservationTransitionException if the reservation is CONFIRMED
     */
    fun release(reservation: Reservation) {
        require(reservation.lotId == lotId) {
            "Reservation ${reservation.id} belongs to lot ${reservation.lotId}, not $lotId"
        }

        if (reservation.release()) {
            available += reservation.quantity
        }
    }
}
