package com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

// Body of POST /events. Annotations use @field: so Bean Validation sees them on the backing field.
@Schema(description = "Data to create an event. It is created in DRAFT status, with no sectors.")
data class CreateEventRequestDTO(
    @field:Schema(description = "Id of the organizer who owns the event.", example = "5d9e2a71-0c3b-4f8e-a6d4-1b2c3d4e5f60", requiredMode = Schema.RequiredMode.REQUIRED)
    val organizerId: UUID,

    @field:Schema(description = "Event name.", example = "Rock in Caraíva 2027", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
    @field:NotBlank
    @field:Size(max = 200)
    val name: String,

    @field:Schema(description = "Venue where the event happens.", example = "Praia de Caraíva", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
    @field:NotBlank
    @field:Size(max = 200)
    val venue: String,

    @field:Schema(description = "City of the venue.", example = "Porto Seguro", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
    @field:NotBlank
    @field:Size(max = 100)
    val city: String,

    @field:Schema(description = "When the event starts, ISO-8601 in UTC. Must be in the future.", example = "2027-03-20T23:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:Future
    val startsAt: Instant,
)
