package com.reynanfc.ticketrush.catalog.application.port.`in`.command

import java.time.Instant
import java.util.UUID

data class AddLotCommand(
    val eventId: UUID,
    val sectorId: UUID,
    val name: String,
    val priceCents: Long,
    val quantity: Int,
    val sortOrder: Int,
    val startsAt: Instant,
    val endsAt: Instant,
)
