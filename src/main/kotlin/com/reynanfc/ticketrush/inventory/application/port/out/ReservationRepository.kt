package com.reynanfc.ticketrush.inventory.application.port.out

import com.reynanfc.ticketrush.inventory.domain.model.Reservation
import java.util.UUID

interface ReservationRepository {

    fun save(reservation: Reservation)
    fun findById(id: UUID): Reservation?
}
