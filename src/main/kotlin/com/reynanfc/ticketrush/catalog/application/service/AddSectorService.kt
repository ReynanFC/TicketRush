package com.reynanfc.ticketrush.catalog.application.service

import com.reynanfc.ticketrush.catalog.application.port.`in`.command.AddSectorCommand
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.AddSectorUseCase
import com.reynanfc.ticketrush.catalog.application.port.out.EventRepository
import com.reynanfc.ticketrush.catalog.domain.exception.EventNotFoundException
import com.reynanfc.ticketrush.catalog.domain.model.Sector

class AddSectorService(
    private val eventRepository: EventRepository,
) : AddSectorUseCase {

    override fun addSector(command: AddSectorCommand): Sector {

        val event = eventRepository.findById(command.eventId)
            ?: throw EventNotFoundException(command.eventId)

        val sector = Sector(name = command.name)
        event.addSector(sector)
        eventRepository.save(event)

        return sector
    }
}