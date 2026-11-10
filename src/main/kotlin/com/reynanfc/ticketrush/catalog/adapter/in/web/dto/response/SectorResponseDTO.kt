package com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "A sector of an event with its lots.")
data class SectorResponseDTO(
    @field:Schema(description = "Sector id.", example = "8a1c2d3e-4f50-4617-8293-a4b5c6d7e8f9")
    val id: UUID,

    @field:Schema(description = "Sector name.", example = "Pista")
    val name: String,

    @field:Schema(description = "Lots of the sector. Empty right after creation.")
    val lots: List<LotResponseDTO>,
)
