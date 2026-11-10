package com.reynanfc.ticketrush.catalog.application.service

import com.reynanfc.ticketrush.catalog.application.port.`in`.command.AddLotCommand
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.AddLotUseCase
import com.reynanfc.ticketrush.catalog.application.port.out.EventRepository
import com.reynanfc.ticketrush.catalog.domain.exception.EventNotFoundException
import com.reynanfc.ticketrush.catalog.domain.model.Lot
import org.slf4j.LoggerFactory

class AddLotService(
    private val eventRepository: EventRepository,
) : AddLotUseCase {

    override fun addLot(command: AddLotCommand): Lot {
        log.debug(
            "Adding lot: eventId={}, sectorId={}, sortOrder={}, window=[{}, {})",
            command.eventId, command.sectorId, command.sortOrder, command.startsAt, command.endsAt,
        )

        val event = eventRepository.findById(command.eventId)
            ?: throw EventNotFoundException(command.eventId)

        val lot = Lot(
            name = command.name,
            priceCents = command.priceCents,
            quantity = command.quantity,
            sortOrder = command.sortOrder,
            startsAt = command.startsAt,
            endsAt = command.endsAt,
        )

        event.addLot(command.sectorId, lot)
        eventRepository.save(event)
        log.info(
            "Lot added: eventId={}, sectorId={}, lotId={}, sortOrder={}, priceCents={}, quantity={}",
            event.id, command.sectorId, lot.id, lot.sortOrder, lot.priceCents, lot.quantity,
        )

        return lot
    }

    companion object {
        private val log = LoggerFactory.getLogger(AddLotService::class.java)
    }
}

