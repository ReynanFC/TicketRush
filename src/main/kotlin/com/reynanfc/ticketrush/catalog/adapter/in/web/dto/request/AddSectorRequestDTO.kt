package com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

// Body of POST /events/{eventId}/sectors. The eventId comes from the URL.
@Schema(description = "Data to add a sector to an event.")
data class AddSectorRequestDTO(
    @field:Schema(description = "Sector name. Must be unique inside the event.", example = "Pista", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
)
