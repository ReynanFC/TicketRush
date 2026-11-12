package com.reynanfc.ticketrush.inventory.domain.exception

import com.reynanfc.ticketrush.inventory.domain.enums.ReservationStatus
import java.util.UUID

class InvalidReservationTransitionException(
    val reservationId: UUID,
    val from: ReservationStatus,
    val to: ReservationStatus,
) : InventoryException("Reservation $reservationId cannot go from $from to $to")
