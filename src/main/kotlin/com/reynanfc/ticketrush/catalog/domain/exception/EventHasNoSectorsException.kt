package com.reynanfc.ticketrush.catalog.domain.exception

import java.util.UUID

class EventHasNoSectorsException(
    val eventId: UUID,
) : CatalogException("Event $eventId cannot be published without sectors")
