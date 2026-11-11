package com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

// JSON shape of an event returned by the API. Changing the domain does not change this contract.
@Schema(description = "An event with its sectors and lots.")
data class EventResponseDTO(
    @field:Schema(description = "Event id.", example = "3f2b8c1e-7a4d-4e9b-9c1a-2d5e6f708192")
    val id: UUID,

    @field:Schema(description = "Id of the organizer who owns the event.", example = "5d9e2a71-0c3b-4f8e-a6d4-1b2c3d4e5f60")
    val organizerId: UUID,

    @field:Schema(description = "Event name.", example = "Rock in Caraíva 2027")
    val name: String,

    @field:Schema(description = "Venue where the event happens.", example = "Praia de Caraíva")
    val venue: String,

    @field:Schema(description = "City of the venue.", example = "Porto Seguro")
    val city: String,

    @field:Schema(description = "When the event starts, ISO-8601 in UTC.", example = "2027-03-20T23:00:00Z")
    val startsAt: Instant,

    @field:Schema(
        description = "DRAFT: still being configured. PUBLISHED: visible and read-only. CLOSED: sales ended.",
        example = "DRAFT",
        allowableValues = ["DRAFT", "PUBLISHED", "CLOSED"],
    )
    val status: String,

    @field:Schema(description = "Sectors of the event. Empty right after creation.")
    val sectors: List<SectorResponseDTO>,
)
