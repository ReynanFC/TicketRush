package com.reynanfc.ticketrush.catalog.domain

import com.reynanfc.ticketrush.catalog.domain.exception.DuplicateSectorNameException
import com.reynanfc.ticketrush.catalog.domain.exception.EventHasNoSectorsException
import com.reynanfc.ticketrush.catalog.domain.exception.EventNotInDraftException
import com.reynanfc.ticketrush.catalog.domain.exception.SectorHasNoLotsException
import com.reynanfc.ticketrush.catalog.fixtures.EventFixtures.anEvent
import com.reynanfc.ticketrush.catalog.fixtures.LotFixtures.aLot
import com.reynanfc.ticketrush.catalog.fixtures.SectorFixtures.aSector
import org.junit.jupiter.api.Nested
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
