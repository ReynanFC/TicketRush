package com.reynanfc.ticketrush.catalog.domain.exception

import com.reynanfc.ticketrush.catalog.domain.enums.EventStatus
import java.util.UUID

class EventNotInDraftException(
    val eventId: UUID,
    val status: EventStatus,
) : CatalogException("Event $eventId must be DRAFT, but is $status")
