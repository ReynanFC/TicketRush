package com.reynanfc.ticketrush.inventory.fakes

import com.reynanfc.ticketrush.inventory.application.port.out.ReservationRepository
import com.reynanfc.ticketrush.inventory.domain.model.Reservation
import java.util.UUID

// Fake adapter for the ReservationRepository port: keeps reservations in memory, no database.
// Stores a copy, so a test only sees a change after save() (the issue #8 lesson).
class InMemoryReservationRepository : ReservationRepository {

    private val reservations = mutableMapOf<UUID, Reservation>()

    override fun save(reservation: Reservation) {
        reservations[reservation.id] = reservation.copy()
    }

    override fun findById(id: UUID): Reservation? = reservations[id]?.copy()

    // Test-only helper, not part of the port.
    fun count(): Int = reservations.size

    private fun Reservation.copy() = Reservation.restore(id, orderId, lotId, quantity, expiresAt, status)
}
