package com.reynanfc.ticketrush.catalog.application.service

import com.reynanfc.ticketrush.catalog.application.port.`in`.command.AddSectorCommand
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.AddSectorUseCase
import com.reynanfc.ticketrush.catalog.application.port.out.EventRepository
import com.reynanfc.ticketrush.catalog.domain.exception.EventNotFoundException
import com.reynanfc.ticketrush.catalog.domain.model.Sector
import org.slf4j.LoggerFactory

class AddSectorService(
    private val eventRepository: EventRepository,
) : AddSectorUseCase {

    override fun addSector(command: AddSectorCommand): Sector {
        log.debug("Adding sector: eventId={}, name='{}'", command.eventId, command.name)

        val event = eventRepository.findById(command.eventId)
            ?: throw EventNotFoundException(command.eventId)

        val sector = Sector(name = command.name)
        event.addSector(sector)
        eventRepository.save(event)
        log.info("Sector added: eventId={}, sectorId={}, name='{}'", event.id, sector.id, sector.name)

        return sector
    }

    companion object {
        private val log = LoggerFactory.getLogger(AddSectorService::class.java)
    }
}
