package com.reynanfc.ticketrush.catalog.fixtures

import com.reynanfc.ticketrush.catalog.application.port.`in`.CreateEventCommand
import com.reynanfc.ticketrush.catalog.fixtures.EventFixtures.EVENT_STARTS_AT
import com.reynanfc.ticketrush.catalog.fixtures.EventFixtures.ORGANIZER_ID
import java.time.Instant
import java.util.UUID

// Fake data for tests: a valid command by default.
object CreateEventCommandFixtures {

    fun aCreateEventCommand(
        organizerId: UUID = ORGANIZER_ID,
        name: String = "Festival de Verão",
        venue: String = "Arena Central",
        city: String = "Salvador",
        startsAt: Instant = EVENT_STARTS_AT,
    ) = CreateEventCommand(
        organizerId = organizerId,
        name = name,
        venue = venue,
        city = city,
        startsAt = startsAt,
    )
}
