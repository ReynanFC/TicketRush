package com.reynanfc.ticketrush.catalog.adapter.`in`.web.mapper

import com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.request.AddLotRequestDTO
import com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.request.AddSectorRequestDTO
import com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.request.CreateEventRequestDTO
import com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.response.EventResponseDTO
import com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.response.LotResponseDTO
import com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.response.SectorResponseDTO
import com.reynanfc.ticketrush.catalog.application.port.`in`.command.AddLotCommand
import com.reynanfc.ticketrush.catalog.application.port.`in`.command.AddSectorCommand
import com.reynanfc.ticketrush.catalog.application.port.`in`.command.CreateEventCommand
import com.reynanfc.ticketrush.catalog.domain.model.Event
import com.reynanfc.ticketrush.catalog.domain.model.Lot
import com.reynanfc.ticketrush.catalog.domain.model.Sector
import java.util.UUID

// Translates between the API's shape and the core: request DTO -> command on the way in, domain -> response DTO on the way out.
// Ids that come from the URL are passed as parameters, never read from the body.
object EventWebMapper {

    fun toCommand(request: CreateEventRequestDTO) = CreateEventCommand(
        organizerId = request.organizerId,
        name = request.name,
        venue = request.venue,
        city = request.city,
        startsAt = request.startsAt,
    )

    fun toCommand(eventId: UUID, request: AddSectorRequestDTO) = AddSectorCommand(
        eventId = eventId,
        name = request.name,
    )

    fun toCommand(eventId: UUID, sectorId: UUID, request: AddLotRequestDTO) = AddLotCommand(
        eventId = eventId,
        sectorId = sectorId,
        name = request.name,
        priceCents = request.priceCents,
        quantity = request.quantity,
        sortOrder = request.sortOrder,
        startsAt = request.startsAt,
        endsAt = request.endsAt,
    )

    fun toResponse(event: Event) = EventResponseDTO(
        id = event.id,
        organizerId = event.organizerId,
        name = event.name,
        venue = event.venue,
        city = event.city,
        startsAt = event.startsAt,
        status = event.status.name,
        sectors = event.sectors.map(::toResponse),
    )

    fun toResponse(sector: Sector) = SectorResponseDTO(
        id = sector.id,
        name = sector.name,
        lots = sector.lots.map(::toResponse),
    )

    fun toResponse(lot: Lot) = LotResponseDTO(
        id = lot.id,
        name = lot.name,
        priceCents = lot.priceCents,
        quantity = lot.quantity,
        sortOrder = lot.sortOrder,
        startsAt = lot.startsAt,
        endsAt = lot.endsAt,
    )
}
