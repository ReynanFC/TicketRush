package com.reynanfc.ticketrush.catalog.application.port.`in`

import com.reynanfc.ticketrush.catalog.domain.model.Event

// Input port: what the catalog offers to the outside world (a controller, a test...).
interface CreateEventUseCase {
    fun create(command: CreateEventCommand): Event
}
