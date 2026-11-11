package com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "A ticket lot. On sale during [startsAt, endsAt).")
data class LotResponseDTO(
    @field:Schema(description = "Lot id.", example = "c4d5e6f7-0819-4a2b-8c3d-4e5f60718293")
    val id: UUID,

    @field:Schema(description = "Lot name.", example = "1º lote")
    val name: String,

    @field:Schema(description = "Ticket price in cents. 15000 = R$ 150,00.", example = "15000")
    val priceCents: Long,

    @field:Schema(description = "Number of tickets in the lot.", example = "500")
    val quantity: Int,

    @field:Schema(description = "Position of the lot in the sector.", example = "1")
    val sortOrder: Int,

    @field:Schema(description = "Start of the sale window (inclusive), ISO-8601 in UTC.", example = "2027-01-10T15:00:00Z")
    val startsAt: Instant,

    @field:Schema(description = "End of the sale window (exclusive), ISO-8601 in UTC.", example = "2027-02-10T15:00:00Z")
    val endsAt: Instant,
)
