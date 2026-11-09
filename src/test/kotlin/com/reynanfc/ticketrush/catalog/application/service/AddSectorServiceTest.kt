package com.reynanfc.ticketrush.catalog.application.service

import com.reynanfc.ticketrush.catalog.domain.exception.DuplicateSectorNameException
import com.reynanfc.ticketrush.catalog.domain.exception.EventNotFoundException
import com.reynanfc.ticketrush.catalog.domain.exception.EventNotInDraftException
import com.reynanfc.ticketrush.catalog.fakes.InMemoryEventRepository
import com.reynanfc.ticketrush.catalog.fixtures.AddSectorCommandFixtures.anAddSectorCommand
import com.reynanfc.ticketrush.catalog.fixtures.EventFixtures.anEvent
import com.reynanfc.ticketrush.catalog.fixtures.LotFixtures.aLot
import com.reynanfc.ticketrush.catalog.fixtures.SectorFixtures.aSector
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class AddSectorServiceTest {

    // A new instance per test (JUnit lifecycle), so each test starts with an empty repository.
    private val repository = InMemoryEventRepository()
    private val service = AddSectorService(repository)

    @Test
    fun `should add the sector to the event`() {
        val event = anEvent().also { repository.save(it) }

        service.addSector(anAddSectorCommand(event.id))
        val updatedEvent = repository.findById(event.id)!!

        assertNotNull(repository.findById(event.id))
        assertEquals(1, event.sectors.size)
        assertEquals("Pista", updatedEvent.sectors.first().name)
    }

    @Test
    fun `should reject a sector for an event that does not exist`() {
        val missingEventId = UUID.randomUUID()

        assertFailsWith<EventNotFoundException> {
            service.addSector(anAddSectorCommand(missingEventId))
        }
    }

    @Test
    fun `should reject a duplicate sector name`() {
        val event = anEvent().also { repository.save(it) }

        service.addSector(anAddSectorCommand(event.id))

        assertFailsWith<DuplicateSectorNameException> {
            service.addSector(anAddSectorCommand(event.id))
        }
    }

    @Test
    fun `should reject a sector for a published event`() {
        val event = anEvent().apply {
            addSector(aSector(lots = listOf(aLot())))
            publish()
        }
        repository.save(event)

        // A different name, so the failure can only come from the status.
        assertFailsWith<EventNotInDraftException> {
            service.addSector(anAddSectorCommand(event.id, name = "Camarote"))
        }
    }
}
