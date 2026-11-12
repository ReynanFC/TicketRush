package com.reynanfc.ticketrush.inventory.domain.model

import com.reynanfc.ticketrush.inventory.domain.enums.ReservationStatus
import com.reynanfc.ticketrush.inventory.domain.exception.InvalidReservationTransitionException
import java.time.Instant
import java.util.UUID

// Tickets held for one order while it is paid. Created by LotStock.reserve(), not directly.
// State machine: MODELAGEM.md, section 5.2.
class Reservation(
    val id: UUID = UUID.randomUUID(),
    val orderId: UUID,
    val lotId: UUID,
    val quantity: Int,
    val expiresAt: Instant,
) {
    var status: ReservationStatus = ReservationStatus.PENDING
        private set

    init {
        require(quantity > 0) { "Quantity must be positive" }
    }

    companion object {
        // Rebuilds a reservation that already exists in storage, keeping its status.
        fun restore(
            id: UUID,
            orderId: UUID,
            lotId: UUID,
            quantity: Int,
            expiresAt: Instant,
            status: ReservationStatus,
        ): Reservation {
            return Reservation(
                id = id,
                orderId = orderId,
                lotId = lotId,
                quantity = quantity,
                expiresAt = expiresAt,
            ).apply {
                this.status = status
            }
        }
    }

    /**
     * PENDING -> CONFIRMED. Confirming again is a no-op (the same message can arrive twice).
     *
     * @throws InvalidReservationTransitionException if the reservation is EXPIRED or RELEASED
     */
    fun confirm() {
        when(status) {
            ReservationStatus.PENDING -> {
                status = ReservationStatus.CONFIRMED
            }
            ReservationStatus.CONFIRMED -> return
            ReservationStatus.EXPIRED, ReservationStatus.RELEASED -> {
                throw InvalidReservationTransitionException(id, status, ReservationStatus.CONFIRMED)
            }
        }
    }

    /**
     * PENDING -> EXPIRED. Any other status is left untouched.
     *
     * @return true if the reservation expired now and its tickets must go back to the stock
     */
    fun expire(): Boolean {
        if (status != ReservationStatus.PENDING) return false

        status = ReservationStatus.EXPIRED

        return true
    }

    /**
     * PENDING -> RELEASED. EXPIRED and RELEASED are left untouched.
     *
     * @return true if the reservation was released now and its tickets must go back to the stock
     * @throws InvalidReservationTransitionException if the reservation is CONFIRMED
     */
    fun release(): Boolean {
        return when (status) {
            ReservationStatus.PENDING -> {
                status = ReservationStatus.RELEASED
                true
            }
            ReservationStatus.EXPIRED, ReservationStatus.RELEASED -> false
            ReservationStatus.CONFIRMED -> {
                throw InvalidReservationTransitionException(id, status, ReservationStatus.RELEASED)
            }
        }
    }
}
