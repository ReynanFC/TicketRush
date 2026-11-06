package com.reynanfc.ticketrush.catalog.domain

import java.util.UUID

class Sector(
    val id: UUID = UUID.randomUUID(),
    val name: String
) {
    private val _lots = mutableListOf<Lot>()
    val lots: List<Lot> get() = _lots

    init {
        require(name.isNotBlank()) { "Name cannot be blank" }
    }

    /**
     * Adds a lot to this sector after validating its constraints.
     *
     * The lot must have a unique sort order and its time window must not
     * overlap with any existing lot.
     *
     * @param lot the lot to be added
     * @throws DuplicateLotSortOrderException if the sort order is already in use
     * @throws OverlappingLotWindowException if the lot's time window overlaps with an existing lot
     */
    fun addLot(lot: Lot) {
        if (lots.any { it.sortOrder == lot.sortOrder }) {
            throw DuplicateLotSortOrderException(lot.sortOrder)
        }

        lots.firstOrNull { it.overlaps(lot) }?.let { conflicting ->
            throw OverlappingLotWindowException(lot, conflicting)
        }

        _lots.add(lot)
    }
}
