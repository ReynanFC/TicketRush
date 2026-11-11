package com.reynanfc.ticketrush.catalog.fixtures

import com.reynanfc.ticketrush.catalog.application.port.`in`.command.AddLotCommand
import com.reynanfc.ticketrush.catalog.fixtures.LotFixtures.LOT_ENDS_AT
import com.reynanfc.ticketrush.catalog.fixtures.LotFixtures.LOT_STARTS_AT
import java.time.Instant
import java.util.UUID

// Fake data for tests: eventId and sectorId are required because they must point to data saved in the test.
object AddLotCommandFixtures {

    fun anAddLotCommand(
        eventId: UUID,
        sectorId: UUID,
        name: String = "1º lote",
        priceCents: Long = 10_000,
        quantity: Int = 100,
        sortOrder: Int = 1,
        startsAt: Instant = LOT_STARTS_AT,
        endsAt: Instant = LOT_ENDS_AT,
    ) = AddLotCommand(
        eventId = eventId,
        sectorId = sectorId,
        name = name,
        priceCents = priceCents,
        quantity = quantity,
        sortOrder = sortOrder,
        startsAt = startsAt,
        endsAt = endsAt,
    )
}
