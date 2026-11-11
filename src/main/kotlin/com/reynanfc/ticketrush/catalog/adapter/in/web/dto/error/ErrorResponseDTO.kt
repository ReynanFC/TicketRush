package com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.error

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

// Body returned for any error that is not about invalid fields (not found, business rule, server error).
@Schema(description = "Error returned by the API.")
data class ErrorResponseDTO(
    @field:Schema(description = "When the error happened, ISO-8601 in UTC.", example = "2026-11-10T13:45:12.345Z")
    val timestamp: Instant,

    @field:Schema(description = "HTTP status code.", example = "422")
    val status: Int,

    @field:Schema(description = "HTTP status name.", example = "Unprocessable Entity")
    val error: String,

    @field:Schema(description = "What went wrong, readable by the user.", example = "A sector named 'Pista' already exists in the event")
    val message: String,

    @field:Schema(description = "Path of the request that failed.", example = "/events/3f2b8c1e-7a4d-4e9b-9c1a-2d5e6f708192/sectors")
    val path: String,
)
