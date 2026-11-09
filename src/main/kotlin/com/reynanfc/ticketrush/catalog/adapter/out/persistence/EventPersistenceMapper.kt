package com.reynanfc.ticketrush.catalog.adapter.out.persistence

import com.reynanfc.ticketrush.catalog.adapter.out.persistence.entity.EventEntity
import com.reynanfc.ticketrush.catalog.adapter.out.persistence.entity.LotEntity
import com.reynanfc.ticketrush.catalog.adapter.out.persistence.entity.SectorEntity
import com.reynanfc.ticketrush.catalog.domain.model.Event
import com.reynanfc.ticketrush.catalog.domain.model.Lot
import com.reynanfc.ticketrush.catalog.domain.model.Sector

// Translates the Event aggregate to its database shape and back.
// Going back uses restore(), so a PUBLISHED event is rebuilt without replaying the domain rules.
object EventPersistenceMapper {

    fun toEntity(event: Event) = EventEntity(
        id = event.id,
        organizerId = event.organizerId,
        name = event.name,
        venue = event.venue,
        city = event.city,
        startsAt = event.startsAt,
        status = event.status,
        sectors = event.sectors.map(::toEntity).toMutableList(),
    )

    fun toDomain(entity: EventEntity) = Event.restore(
        id = entity.id,
        organizerId = entity.organizerId,
        name = entity.name,
        venue = entity.venue,
        city = entity.city,
        startsAt = entity.startsAt,
        status = entity.status,
        sectors = entity.sectors.map(::toDomain),
    )

    private fun toEntity(sector: Sector) = SectorEntity(
        id = sector.id,
        name = sector.name,
        lots = sector.lots.map(::toEntity).toMutableList(),
    )

    private fun toDomain(entity: SectorEntity) = Sector.restore(
        id = entity.id,
        name = entity.name,
        lots = entity.lots.map(::toDomain),
    )

    private fun toEntity(lot: Lot) = LotEntity(
        id = lot.id,
        name = lot.name,
        priceCents = lot.priceCents,
        quantity = lot.quantity,
        sortOrder = lot.sortOrder,
        startsAt = lot.startsAt,
        endsAt = lot.endsAt,
    )

    private fun toDomain(entity: LotEntity) = Lot(
        id = entity.id,
        name = entity.name,
        priceCents = entity.priceCents,
        quantity = entity.quantity,
        sortOrder = entity.sortOrder,
        startsAt = entity.startsAt,
        endsAt = entity.endsAt,
    )
}
