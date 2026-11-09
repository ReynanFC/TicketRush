package com.reynanfc.ticketrush.catalog.domain.model

import com.reynanfc.ticketrush.catalog.domain.enums.EventStatus
import com.reynanfc.ticketrush.catalog.domain.exception.DuplicateLotSortOrderException
import com.reynanfc.ticketrush.catalog.domain.exception.DuplicateSectorNameException
import com.reynanfc.ticketrush.catalog.domain.exception.EventHasNoSectorsException
import com.reynanfc.ticketrush.catalog.domain.exception.EventNotInDraftException
import com.reynanfc.ticketrush.catalog.domain.exception.SectorHasNoLotsException
import com.reynanfc.ticketrush.catalog.domain.exception.SectorNotFoundException
import com.reynanfc.ticketrush.catalog.fixtures.EventFixtures.anEvent
import com.reynanfc.ticketrush.catalog.fixtures.LotFixtures.LOT_ENDS_AT
import com.reynanfc.ticketrush.catalog.fixtures.LotFixtures.aLot
import com.reynanfc.ticketrush.catalog.fixtures.SectorFixtures.aSector
import org.junit.jupiter.api.Nested
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EventTest {

    @Nested
    inner class Creation {

        @Test
        fun `should reject blank name`() {
            assertFailsWith<IllegalArgumentException> { anEvent(name = "  ") }
        }

        @Test
        fun `should reject blank venue`() {
            assertFailsWith<IllegalArgumentException> { anEvent(venue = "  ") }
        }

        @Test
        fun `should reject blank city`() {
            assertFailsWith<IllegalArgumentException> { anEvent(city = "  ") }
        }

        @Test
        fun `should start as draft without sectors`() {
            val event = anEvent()

            assertEquals(EventStatus.DRAFT, event.status)
            assertEquals(emptyList(), event.sectors)
        }
    }

    @Nested
    inner class AddSector {

        @Test
        fun `should accept a sector while draft`() {
            val event = anEvent()
            val sector = aSector(name = "Pista")

            event.addSector(sector)

            assertEquals(listOf(sector), event.sectors)
        }

        @Test
        fun `should reject a sector with a repeated name`() {
            val event = anEvent()
            event.addSector(aSector(name = "Pista"))

            val error = assertFailsWith<DuplicateSectorNameException> {
                event.addSector(aSector(name = "Pista"))
            }

            assertEquals("Pista", error.sectorName)
        }

        @Test
        fun `should reject a sector after the event is published`() {
            val event = anEvent()
            event.addSector(aSector(name = "Pista", lots = listOf(aLot())))
            event.publish()

            val error = assertFailsWith<EventNotInDraftException> {
                event.addSector(aSector(name = "Camarote"))
            }

            assertEquals(EventStatus.PUBLISHED, error.status)
        }
    }

    @Nested
    inner class AddLot {

        @Test
        fun `should add the lot to the given sector`() {
            val event = anEvent()
            val pista = aSector(name = "Pista")
            val camarote = aSector(name = "Camarote")
            event.addSector(pista)
            event.addSector(camarote)
            val lot = aLot()

            event.addLot(camarote.id, lot)

            assertEquals(listOf(lot), camarote.lots)
            assertEquals(emptyList(), pista.lots)
        }

        @Test
        fun `should reject a lot for a sector that is not in the event`() {
            val event = anEvent()
            event.addSector(aSector(name = "Pista"))
            val missingSectorId = UUID.randomUUID()

            val error = assertFailsWith<SectorNotFoundException> {
                event.addLot(missingSectorId, aLot())
            }

            assertEquals(missingSectorId, error.sectorId)
        }

        @Test
        fun `should reject a lot after the event is published`() {
            val event = anEvent()
            val sector = aSector(name = "Pista", lots = listOf(aLot(sortOrder = 1)))
            event.addSector(sector)
            event.publish()

            assertFailsWith<EventNotInDraftException> {
                event.addLot(sector.id, aLot(sortOrder = 2, startsAt = LOT_ENDS_AT, endsAt = LOT_ENDS_AT.plusSeconds(3600)))
            }
            assertEquals(1, sector.lots.size)
        }

        @Test
        fun `should keep the sector rules when adding through the event`() {
            val event = anEvent()
            val sector = aSector(name = "Pista", lots = listOf(aLot(sortOrder = 1)))
            event.addSector(sector)

            assertFailsWith<DuplicateLotSortOrderException> {
                event.addLot(sector.id, aLot(sortOrder = 1))
            }
        }
    }

    @Nested
    inner class Publish {

        @Test
        fun `should publish a complete event`() {
            val event = anEvent()
            event.addSector(aSector(name = "Pista", lots = listOf(aLot())))

            event.publish()

            assertEquals(EventStatus.PUBLISHED, event.status)
        }

        @Test
        fun `should reject an event without sectors`() {
            val event = anEvent()

            assertFailsWith<EventHasNoSectorsException> { event.publish() }
        }

        @Test
        fun `should reject an event with a sector without lots`() {
            val event = anEvent()
            event.addSector(aSector(name = "Pista", lots = listOf(aLot())))
            event.addSector(aSector(name = "Camarote"))

            val error = assertFailsWith<SectorHasNoLotsException> { event.publish() }

            assertEquals("Camarote", error.sectorName)
        }

        @Test
        fun `should reject publishing twice`() {
            val event = anEvent()
            event.addSector(aSector(name = "Pista", lots = listOf(aLot())))
            event.publish()

            assertFailsWith<EventNotInDraftException> { event.publish() }
        }

        @Test
        fun `should keep draft status when publishing fails`() {
            val event = anEvent()

            runCatching { event.publish() }

            assertEquals(EventStatus.DRAFT, event.status)
        }
    }
}
