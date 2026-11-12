package com.reynanfc.ticketrush.inventory.fixtures

import com.reynanfc.ticketrush.inventory.application.port.`in`.command.ReserveTicketsCommand
import com.reynanfc.ticketrush.inventory.fixtures.LotStockFixtures.LOT_ID
import com.reynanfc.ticketrush.inventory.fixtures.ReservationFixtures.ORDER_ID
import java.util.UUID

// Fake data for tests: valid by default; each test overrides only what matters.
object ReserveTicketsCommandFixtures {

    fun aReserveTicketsCommand(
        orderId: UUID = ORDER_ID,
        lotId: UUID = LOT_ID,
        quantity: Int = 2,
    ) = ReserveTicketsCommand(
        orderId = orderId,
        lotId = lotId,
        quantity = quantity,
    )
}
