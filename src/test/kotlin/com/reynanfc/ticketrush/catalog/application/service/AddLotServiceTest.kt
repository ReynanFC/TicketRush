package com.reynanfc.ticketrush.catalog.application.service

import com.reynanfc.ticketrush.catalog.domain.exception.EventNotFoundException
import com.reynanfc.ticketrush.catalog.domain.exception.EventNotInDraftException
import com.reynanfc.ticketrush.catalog.domain.exception.SectorNotFoundException
import com.reynanfc.ticketrush.catalog.domain.model.Event
import com.reynanfc.ticketrush.catalog.fakes.InMemoryEventRepository
import com.reynanfc.ticketrush.catalog.fixtures.AddLotCommandFixtures.anAddLotCommand
import com.reynanfc.ticketrush.catalog.fixtures.EventFixtures.anEvent
import com.reynanfc.ticketrush.catalog.fixtures.LotFixtures.aLot
import com.reynanfc.ticketrush.catalog.fixtures.SectorFixtures.aSector
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class AddLotServiceTest {

    // A new instance per test (JUnit lifecycle), so each test starts with an empty repository.
    private val repository = InMemoryEventRepository()
    private val service = AddLotService(repository)

    // A saved DRAFT event with one empty sector named "Pista".
    private fun aSavedEventWithSector(): Event =
        anEvent().apply { addSector(aSector(name = "Pista")) }.also { repository.save(it) }

    @Test
    fun `should add the lot to the sector`() {
        val event = aSavedEventWithSector()
        val sectorId = event.sectors.first().id

        val lot = service.addLot(anAddLotCommand(event.id, sectorId, name = "Promocional"))

        val updatedEvent = assertNotNull(repository.findById(event.id))
        val updatedSector = updatedEvent.sectors.first { it.id == sectorId }
        assertEquals(listOf(lot.id), updatedSector.lots.map { it.id })
        assertEquals("Promocional", lot.name)
    }

    @Test
    fun `should reject a lot for an event that does not exist`() {
        val missingEventId = UUID.randomUUID()

        assertFailsWith<EventNotFoundException> {
            service.addLot(anAddLotCommand(missingEventId, sectorId = UUID.randomUUID()))
        }
    }

    @Test
    fun `should reject a lot for a sector that does not exist`() {
        val event = aSavedEventWithSector()
        val missingSectorId = UUID.randomUUID()

        assertFailsWith<SectorNotFoundException> {
            service.addLot(anAddLotCommand(event.id, missingSectorId))
        }
    }

    @Test
    fun `should reject a lot for a published event`() {
        val sector = aSector(name = "Pista", lots = listOf(aLot(sortOrder = 1)))
        val event = anEvent().apply {
            addSector(sector)
            publish()
        }
        repository.save(event)

        // Sort order 2 and a later window, so the failure can only come from the status.
        assertFailsWith<EventNotInDraftException> {
            service.addLot(
                anAddLotCommand(
                    event.id,
                    sector.id,
                    sortOrder = 2,
                    startsAt = sector.lots.first().endsAt,
                    endsAt = sector.lots.first().endsAt.plusSeconds(3600),
                )
            )
        }
    }

    @Test
    fun `should reject an invalid lot without saving it`() {
        val event = aSavedEventWithSector()
        val sectorId = event.sectors.first().id

        assertFailsWith<IllegalArgumentException> {
            service.addLot(anAddLotCommand(event.id, sectorId, quantity = 0))
        }
        assertEquals(emptyList(), event.sectors.first().lots)
    }
}
