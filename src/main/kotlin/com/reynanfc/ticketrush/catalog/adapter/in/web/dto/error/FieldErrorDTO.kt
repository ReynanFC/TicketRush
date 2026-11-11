package com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.error

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "One invalid field of the request.")
data class FieldErrorDTO(
    @field:Schema(description = "Name of the field, as sent in the JSON.", example = "name")
    val field: String,

    @field:Schema(description = "Why the value was rejected.", example = "must not be blank")
    val message: String,

    @field:Schema(description = "Value that was sent. Null when the field was missing.", example = " ", nullable = true)
    val rejectedValue: Any?,
)
