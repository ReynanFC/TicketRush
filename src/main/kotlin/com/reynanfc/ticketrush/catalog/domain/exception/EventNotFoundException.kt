package com.reynanfc.ticketrush.catalog.domain.exception

import java.util.UUID

class EventNotFoundException(
    val eventId: UUID,
) : CatalogException("Event $eventId not found")
