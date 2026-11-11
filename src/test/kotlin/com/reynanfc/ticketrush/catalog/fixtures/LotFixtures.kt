package com.reynanfc.ticketrush.catalog.fixtures

import com.reynanfc.ticketrush.catalog.domain.model.Lot
import java.time.Instant

// Fake data for tests: valid by default; each test overrides only what matters.
object LotFixtures {

    val LOT_STARTS_AT: Instant = Instant.parse("2026-12-01T18:00:00Z")
    val LOT_ENDS_AT: Instant = Instant.parse("2026-12-10T18:00:00Z")

    fun aLot(
        name: String = "1º lote",
        priceCents: Long = 10_000,
        quantity: Int = 100,
        sortOrder: Int = 1,
        startsAt: Instant = LOT_STARTS_AT,
        endsAt: Instant = LOT_ENDS_AT,
    ) = Lot(
        name = name,
        priceCents = priceCents,
        quantity = quantity,
        sortOrder = sortOrder,
        startsAt = startsAt,
        endsAt = endsAt,
    )
}
