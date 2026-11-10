package com.reynanfc.ticketrush.catalog.application.service

import com.reynanfc.ticketrush.catalog.application.port.`in`.command.PublishEventCommand
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.PublishEventUseCase
import com.reynanfc.ticketrush.catalog.application.port.out.EventRepository
import com.reynanfc.ticketrush.catalog.domain.exception.EventNotFoundException
import com.reynanfc.ticketrush.catalog.domain.model.Event
import org.slf4j.LoggerFactory

class PublishEventService(
    private val eventRepository: EventRepository,
) : PublishEventUseCase {

    override fun publish(command: PublishEventCommand): Event {
        log.debug("Publishing event: eventId={}", command.eventId)

        val event = eventRepository.findById(command.eventId)
            ?: throw EventNotFoundException(command.eventId)

        event.publish()
        eventRepository.save(event)
        log.info("Event published: eventId={}, sectors={}, lots={}", event.id, event.sectors.size, event.sectors.sumOf { it.lots.size })

        return event
    }

    companion object {
        private val log = LoggerFactory.getLogger(PublishEventService::class.java)
    }
}

