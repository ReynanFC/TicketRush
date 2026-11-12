package com.reynanfc.ticketrush.catalog.domain.model

import com.reynanfc.ticketrush.catalog.domain.enums.EventStatus
import com.reynanfc.ticketrush.catalog.domain.exception.DuplicateLotSortOrderException
import com.reynanfc.ticketrush.catalog.domain.exception.DuplicateSectorNameException
import com.reynanfc.ticketrush.catalog.domain.exception.EventHasNoSectorsException
import com.reynanfc.ticketrush.catalog.domain.exception.EventNotInDraftException
import com.reynanfc.ticketrush.catalog.domain.exception.LotEndsTooLateException
import com.reynanfc.ticketrush.catalog.domain.exception.OverlappingLotWindowException
import com.reynanfc.ticketrush.catalog.domain.exception.SectorHasNoLotsException
import com.reynanfc.ticketrush.catalog.domain.exception.SectorNotFoundException
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

    companion object {
        // Rebuilds an event that already exists in storage, keeping its status.
        // The constructor still runs the input validation; business rules were checked when it was saved.
        fun restore(
            id: UUID,
            organizerId: UUID,
            name: String,
            venue: String,
            city: String,
            startsAt: Instant,
            status: EventStatus,
            sectors: List<Sector>,
        ): Event {
            return Event(
                id = id,
                organizerId = organizerId,
                name = name,
                venue = venue,
                city = city,
                startsAt = startsAt,
            ).apply {
                this.status = status
                _sectors.addAll(sectors)
            }
        }
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

    /**
     * Adds a lot to one of the event's sectors. Lots go through the aggregate root,
     * so the DRAFT rule applies to them too.
     *
     * @throws EventNotInDraftException if the event is not DRAFT
     * @throws LotEndsTooLateException if the lot does not end before the event starts
     * @throws SectorNotFoundException if the sector is not part of this event
     * @throws DuplicateLotSortOrderException if the sort order is already in use in the sector
     * @throws OverlappingLotWindowException if the lot's window overlaps another lot in the sector
     */
    fun addLot(sectorId: UUID, lot: Lot) {
        ensureDraft()

        if (!lot.endsAt.isBefore(startsAt)) {
            throw LotEndsTooLateException(lot.name, lot.endsAt, startsAt)
        }

        val sector = sectors.firstOrNull { it.id == sectorId }
            ?: throw SectorNotFoundException(sectorId)

        sector.addLot(lot)
    }

    private fun ensureDraft() {
        if (status != EventStatus.DRAFT) {
            throw EventNotInDraftException(id, status)
        }
    }
}
