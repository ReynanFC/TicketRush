package com.reynanfc.ticketrush.catalog.fixtures

import com.reynanfc.ticketrush.catalog.application.port.out.EventRepository
import com.reynanfc.ticketrush.catalog.domain.model.Event
import java.util.UUID

// Fake adapter for the EventRepository port: keeps events in memory, no database.
class InMemoryEventRepository : EventRepository {

    private val events = mutableMapOf<UUID, Event>()

    override fun save(event: Event) {
        events[event.id] = event
    }

    override fun findById(id: UUID): Event? = events[id]

    // Test-only helper, not part of the port.
    fun count(): Int = events.size
}
