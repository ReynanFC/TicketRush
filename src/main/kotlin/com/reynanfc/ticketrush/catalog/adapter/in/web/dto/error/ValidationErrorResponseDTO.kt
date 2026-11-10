package com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.error

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

// Body returned when the request body or a path variable has invalid fields. Lists every invalid field at once.
@Schema(description = "Validation error: the request has one or more invalid fields.")
data class ValidationErrorResponseDTO(
    @field:Schema(description = "When the error happened, ISO-8601 in UTC.", example = "2026-11-10T13:45:12.345Z")
    val timestamp: Instant,

    @field:Schema(description = "HTTP status code.", example = "400")
    val status: Int,

    @field:Schema(description = "HTTP status name.", example = "Bad Request")
    val error: String,

    @field:Schema(description = "General message. The details are in fieldErrors.", example = "One or more fields are invalid")
    val message: String,

    @field:Schema(description = "Path of the request that failed.", example = "/events")
    val path: String,

    @field:Schema(description = "Every invalid field, with the reason.")
    val fieldErrors: List<FieldErrorDTO>,
)
