package com.reynanfc.ticketrush.catalog.application.port.`in`

import java.time.Instant
import java.util.UUID

data class CreateEventCommand(
    val organizerId: UUID,
    val name: String,
    val venue: String,
    val city: String,
    val startsAt: Instant
)