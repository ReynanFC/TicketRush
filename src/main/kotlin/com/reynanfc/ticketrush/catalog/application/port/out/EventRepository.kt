package com.reynanfc.ticketrush.catalog.application.port.out

import com.reynanfc.ticketrush.catalog.domain.model.Event
import java.util.UUID

interface EventRepository {

    fun save(event: Event)
    fun findById(id: UUID): Event?
}