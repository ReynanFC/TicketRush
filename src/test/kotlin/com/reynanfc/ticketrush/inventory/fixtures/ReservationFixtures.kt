package com.reynanfc.ticketrush.inventory.fixtures

import com.reynanfc.ticketrush.inventory.domain.enums.ReservationStatus
import com.reynanfc.ticketrush.inventory.domain.model.Reservation
import com.reynanfc.ticketrush.inventory.fixtures.LotStockFixtures.LOT_ID
import java.time.Instant
import java.util.UUID

// Fake data for tests: valid by default; each test overrides only what matters.
object ReservationFixtures {

    val ORDER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-00000000c001")
    val EXPIRES_AT: Instant = Instant.parse("2026-12-05T18:10:00Z")

    fun aReservation(
        id: UUID = UUID.randomUUID(),
        orderId: UUID = ORDER_ID,
        lotId: UUID = LOT_ID,
        quantity: Int = 2,
        expiresAt: Instant = EXPIRES_AT,
        status: ReservationStatus = ReservationStatus.PENDING,
    ) = Reservation.restore(
        id = id,
        orderId = orderId,
        lotId = lotId,
        quantity = quantity,
        expiresAt = expiresAt,
        status = status,
    )
}
