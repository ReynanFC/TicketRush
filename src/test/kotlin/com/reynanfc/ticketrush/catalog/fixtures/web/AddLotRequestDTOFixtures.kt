package com.reynanfc.ticketrush.catalog.fixtures.web

import com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.request.AddLotRequestDTO
import java.time.Instant

// Dates far in the future for the same reason as CreateEventRequestDTOFixtures (@Future on endsAt).
object AddLotRequestDTOFixtures {

    val REQUEST_LOT_STARTS_AT: Instant = Instant.parse("2030-01-10T15:00:00Z")
    val REQUEST_LOT_ENDS_AT: Instant = Instant.parse("2030-02-10T15:00:00Z")

    fun anAddLotRequest(
        name: String = "1º lote",
        priceCents: Long = 10_000,
        quantity: Int = 100,
        sortOrder: Int = 1,
        startsAt: Instant = REQUEST_LOT_STARTS_AT,
        endsAt: Instant = REQUEST_LOT_ENDS_AT,
    ) = AddLotRequestDTO(
        name = name,
        priceCents = priceCents,
        quantity = quantity,
        sortOrder = sortOrder,
        startsAt = startsAt,
        endsAt = endsAt,
    )
}
