package com.reynanfc.ticketrush.catalog.application.port.`in`.command

import java.util.UUID

data class PublishEventCommand(
    val eventId: UUID,
)
