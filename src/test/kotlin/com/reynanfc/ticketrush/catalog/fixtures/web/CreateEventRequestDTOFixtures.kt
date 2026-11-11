package com.reynanfc.ticketrush.catalog.fixtures.web

import com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.request.CreateEventRequestDTO
import java.time.Instant
import java.util.UUID

// Fake request bodies for web tests. Dates are far in the future because the DTO has @Future:
// the domain fixtures (2026-12) would start failing validation once that date passes.
object CreateEventRequestDTOFixtures {

    val REQUEST_ORGANIZER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val REQUEST_EVENT_STARTS_AT: Instant = Instant.parse("2030-03-20T23:00:00Z")

    fun aCreateEventRequest(
        organizerId: UUID = REQUEST_ORGANIZER_ID,
        name: String = "Festival de Verão",
        venue: String = "Arena Central",
        city: String = "Salvador",
        startsAt: Instant = REQUEST_EVENT_STARTS_AT,
    ) = CreateEventRequestDTO(
        organizerId = organizerId,
        name = name,
        venue = venue,
        city = city,
        startsAt = startsAt,
    )
}
