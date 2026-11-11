package com.reynanfc.ticketrush.catalog.application.service

import com.reynanfc.ticketrush.catalog.application.port.`in`.command.PublishEventCommand
import com.reynanfc.ticketrush.catalog.domain.enums.EventStatus
import com.reynanfc.ticketrush.catalog.domain.exception.EventHasNoSectorsException
import com.reynanfc.ticketrush.catalog.domain.exception.EventNotFoundException
import com.reynanfc.ticketrush.catalog.domain.exception.EventNotInDraftException
import com.reynanfc.ticketrush.catalog.domain.model.Event
import com.reynanfc.ticketrush.catalog.fakes.InMemoryEventRepository
import com.reynanfc.ticketrush.catalog.fixtures.EventFixtures.anEvent
import com.reynanfc.ticketrush.catalog.fixtures.LotFixtures.aLot
import com.reynanfc.ticketrush.catalog.fixtures.SectorFixtures.aSector
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class PublishEventServiceTest {

    // A new instance per test (JUnit lifecycle), so each test starts with an empty repository.
    private val repository = InMemoryEventRepository()
    private val service = PublishEventService(repository)

    // A saved DRAFT event with one sector and one lot: ready to be published.
    private fun aSavedPublishableEvent(): Event =
        anEvent()
            .apply { addSector(aSector(name = "Pista", lots = listOf(aLot()))) }
            .also { repository.save(it) }

    @Test
    fun `should publish a complete event`() {
        val event = aSavedPublishableEvent()

        service.publish(PublishEventCommand(event.id))

        val savedEvent = assertNotNull(repository.findById(event.id))
        assertEquals(EventStatus.PUBLISHED, savedEvent.status)
    }

    @Test
    fun `should reject an event that does not exist`() {
        val missingEventId = UUID.randomUUID()

        assertFailsWith<EventNotFoundException> {
            service.publish(PublishEventCommand(missingEventId))
        }
    }

    @Test
    fun `should keep the event as draft when it has no sectors`() {
        val event = anEvent().also { repository.save(it) }

        assertFailsWith<EventHasNoSectorsException> {
            service.publish(PublishEventCommand(event.id))
        }

        val savedEvent = assertNotNull(repository.findById(event.id))
        assertEquals(EventStatus.DRAFT, savedEvent.status)
    }

    @Test
    fun `should reject publishing twice`() {
        val event = aSavedPublishableEvent()
        service.publish(PublishEventCommand(event.id))

        assertFailsWith<EventNotInDraftException> {
            service.publish(PublishEventCommand(event.id))
        }
    }
}
