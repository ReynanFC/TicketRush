package com.reynanfc.ticketrush.catalog.domain.exception

import java.time.Instant

class LotEndsTooLateException(
    val lotName: String,
    val lotEndsAt: Instant,
    val eventStartsAt: Instant,
) : CatalogException("Lot '$lotName' must end before the event starts at $eventStartsAt, but ends at $lotEndsAt")
