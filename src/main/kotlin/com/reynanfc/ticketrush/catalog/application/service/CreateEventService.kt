package com.reynanfc.ticketrush.catalog.application.service

import com.reynanfc.ticketrush.catalog.application.port.`in`.command.CreateEventCommand
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.CreateEventUseCase
import com.reynanfc.ticketrush.catalog.application.port.out.EventRepository
import com.reynanfc.ticketrush.catalog.domain.model.Event
import org.slf4j.LoggerFactory

class CreateEventService(
    private val eventRepository: EventRepository,
) : CreateEventUseCase {

    override fun create(command: CreateEventCommand): Event {
        log.debug("Creating event: organizerId={}, name='{}', startsAt={}", command.organizerId, command.name, command.startsAt)

        val event = Event(
            organizerId = command.organizerId,
            name = command.name,
            venue = command.venue,
            city = command.city,
            startsAt = command.startsAt
        )

        eventRepository.save(event)
        log.info("Event created: eventId={}, organizerId={}, status={}", event.id, event.organizerId, event.status)

        return event
    }

    companion object {
        private val log = LoggerFactory.getLogger(CreateEventService::class.java)
    }
}
