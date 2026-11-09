package com.reynanfc.ticketrush.catalog.domain.model

import com.reynanfc.ticketrush.catalog.domain.enums.EventStatus
import com.reynanfc.ticketrush.catalog.domain.exception.DuplicateSectorNameException
import com.reynanfc.ticketrush.catalog.domain.exception.EventNotInDraftException
import com.reynanfc.ticketrush.catalog.domain.exception.EventHasNoSectorsException
import com.reynanfc.ticketrush.catalog.domain.exception.SectorHasNoLotsException
import java.time.Instant
import java.util.UUID

// Aggregate root of the catalog: sectors and lots are only changed through it.
class Event(
    val id: UUID = UUID.randomUUID(),
    val organizerId: UUID,
    val name: String,
    val venue: String,
    val city: String,
    val startsAt: Instant,
) {
    var status: EventStatus = EventStatus.DRAFT
        private set

    private val _sectors = mutableListOf<Sector>()
    val sectors: List<Sector> get() = _sectors

    init {
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(venue.isNotBlank()) { "Venue cannot be blank" }
        require(city.isNotBlank()) { "City cannot be blank" }
    }

    /**
     * @throws EventNotInDraftException if the event is not DRAFT
     * @throws DuplicateSectorNameException if the name is already in use
     */
    fun addSector(sector: Sector) {
        ensureDraft()

        if (sectors.any { it.name == sector.name }) {
            throw DuplicateSectorNameException(sector.name)
        }

        _sectors.add(sector)
    }

    /**
     * @throws EventNotInDraftException if the event is not DRAFT
     * @throws EventHasNoSectorsException if there are no sectors
     * @throws SectorHasNoLotsException if any sector has no lots
     */
    fun publish() {
        ensureDraft()

        if (sectors.isEmpty()) {
            throw EventHasNoSectorsException(id)
        }

        sectors.firstOrNull { it.lots.isEmpty() }?.let { sector ->
            throw SectorHasNoLotsException(sector.id, sector.name)
        }

        status = EventStatus.PUBLISHED
    }

    private fun ensureDraft() {
        if (status != EventStatus.DRAFT) {
            throw EventNotInDraftException(id, status)
        }
    }
}
