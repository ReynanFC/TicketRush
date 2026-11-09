package com.reynanfc.ticketrush.catalog.application.service

import com.reynanfc.ticketrush.catalog.application.port.`in`.command.CreateEventCommand
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.CreateEventUseCase
import com.reynanfc.ticketrush.catalog.application.port.out.EventRepository
import com.reynanfc.ticketrush.catalog.domain.model.Event

class CreateEventService(
    private val eventRepository: EventRepository,
) : CreateEventUseCase {

    override fun create(command: CreateEventCommand): Event {
        val event = Event(
            organizerId = command.organizerId,
            name = command.name,
            venue = command.venue,
            city = command.city,
            startsAt = command.startsAt
        )

        eventRepository.save(event)
        return event
    }
}