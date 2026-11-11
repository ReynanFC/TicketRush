package com.reynanfc.ticketrush.catalog.fixtures

import com.reynanfc.ticketrush.catalog.domain.model.Event
import java.time.Instant
import java.util.UUID

// Fake data for tests: a valid DRAFT event without sectors by default.
object EventFixtures {

    val ORGANIZER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val EVENT_STARTS_AT: Instant = Instant.parse("2026-12-20T22:00:00Z")

    fun anEvent(
        organizerId: UUID = ORGANIZER_ID,
        name: String = "Festival de Verão",
        venue: String = "Arena Central",
        city: String = "Salvador",
        startsAt: Instant = EVENT_STARTS_AT,
    ) = Event(
        organizerId = organizerId,
        name = name,
        venue = venue,
        city = city,
        startsAt = startsAt,
    )
}
