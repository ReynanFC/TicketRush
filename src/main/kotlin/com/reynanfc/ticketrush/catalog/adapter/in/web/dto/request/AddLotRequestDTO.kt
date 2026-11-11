package com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.Instant

// Body of POST /events/{eventId}/sectors/{sectorId}/lots. Both ids come from the URL.
// endsAt > startsAt is a cross-field rule and stays in the domain (Lot's init).
@Schema(description = "Data to add a ticket lot to a sector. The sale window is [startsAt, endsAt).")
data class AddLotRequestDTO(
    @field:Schema(description = "Lot name.", example = "1º lote", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:Schema(description = "Ticket price in cents. 15000 = R$ 150,00. Zero means a free ticket.", example = "15000", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
    @field:PositiveOrZero
    val priceCents: Long,

    @field:Schema(description = "Number of tickets in the lot.", example = "500", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
    @field:Positive
    val quantity: Int,

    @field:Schema(description = "Position of the lot in the sector (1º lote, 2º lote...). Must be unique inside the sector.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
    @field:PositiveOrZero
    val sortOrder: Int,

    @field:Schema(description = "Start of the sale window (inclusive), ISO-8601 in UTC.", example = "2027-01-10T15:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    val startsAt: Instant,

    @field:Schema(description = "End of the sale window (exclusive), ISO-8601 in UTC. Must be after startsAt and in the future.", example = "2027-02-10T15:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:Future
    val endsAt: Instant,
)
