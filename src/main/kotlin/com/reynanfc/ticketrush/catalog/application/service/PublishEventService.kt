package com.reynanfc.ticketrush.catalog.application.service

import com.reynanfc.ticketrush.catalog.application.port.`in`.command.PublishEventCommand
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.PublishEventUseCase
import com.reynanfc.ticketrush.catalog.application.port.out.EventRepository
import com.reynanfc.ticketrush.catalog.domain.exception.EventNotFoundException
import com.reynanfc.ticketrush.catalog.domain.model.Event

class PublishEventService(
    private val eventRepository: EventRepository,
) : PublishEventUseCase {

    override fun publish(command: PublishEventCommand): Event {
        val event = eventRepository.findById(command.eventId)
            ?: throw EventNotFoundException(command.eventId)

        event.publish()
        eventRepository.save(event)

        return event
    }
}
