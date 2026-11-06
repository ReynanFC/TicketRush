package com.reynanfc.ticketrush.catalog.domain

import com.reynanfc.ticketrush.catalog.fixtures.LotFixtures.LOT_ENDS_AT
import com.reynanfc.ticketrush.catalog.fixtures.LotFixtures.LOT_STARTS_AT
import com.reynanfc.ticketrush.catalog.fixtures.LotFixtures.aLot
import com.reynanfc.ticketrush.catalog.fixtures.SectorFixtures.aSector
import org.junit.jupiter.api.Nested
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SectorTest {

    @Nested
    inner class Creation {

        @Test
        fun `should reject blank name`() {
            assertFailsWith<IllegalArgumentException> { aSector(name = "  ") }
        }

        @Test
        fun `should start without lots`() {
            val sector = aSector()

            assertEquals(emptyList(), sector.lots)
        }
    }

    @Nested
    inner class AddLot {

        // First lot: [LOT_STARTS_AT, LOT_ENDS_AT), sortOrder 1
        private val firstLot = aLot(sortOrder = 1, startsAt = LOT_STARTS_AT, endsAt = LOT_ENDS_AT)

        @Test
        fun `should accept the first lot`() {
            val sector = aSector()

            sector.addLot(firstLot)

            assertEquals(listOf(firstLot), sector.lots)
        }

        @Test
        fun `should accept a lot that starts exactly when the previous one ends`() {
            val sector = aSector()
            sector.addLot(firstLot)
            val nextLot = aLot(sortOrder = 2, startsAt = LOT_ENDS_AT, endsAt = LOT_ENDS_AT.plus(Duration.ofDays(10)))

            sector.addLot(nextLot)

            assertEquals(listOf(firstLot, nextLot), sector.lots)
        }

        @Test
        fun `should reject a lot with a repeated sortOrder`() {
            val sector = aSector()
            sector.addLot(firstLot)

            val sameOrder = aLot(sortOrder = 1, startsAt = LOT_ENDS_AT, endsAt = LOT_ENDS_AT.plus(Duration.ofDays(10)))

            val error = assertFailsWith<DuplicateLotSortOrderException> { sector.addLot(sameOrder) }

            assertEquals(1, error.sortOrder)
        }

        @Test
        fun `should reject a lot that overlaps by one second`() {
            val sector = aSector()
            sector.addLot(firstLot)
            val overlapping = aLot(sortOrder = 2, startsAt = LOT_ENDS_AT.minusSeconds(1), endsAt = LOT_ENDS_AT.plus(Duration.ofDays(10)))

            val error = assertFailsWith<OverlappingLotWindowException> { sector.addLot(overlapping) }

            assertEquals(firstLot.id, error.conflictingLotId)
        }

        @Test
        fun `should reject a lot entirely inside another lot window`() {
            val sector = aSector()
            sector.addLot(firstLot)
            val inside = aLot(sortOrder = 2, startsAt = LOT_STARTS_AT.plus(Duration.ofDays(1)), endsAt = LOT_ENDS_AT.minus(Duration.ofDays(1)))

            assertFailsWith<OverlappingLotWindowException> { sector.addLot(inside) }
        }

        @Test
        fun `should not keep a rejected lot`() {
            val sector = aSector()
            sector.addLot(firstLot)
            val overlapping = aLot(sortOrder = 2, startsAt = LOT_STARTS_AT, endsAt = LOT_ENDS_AT)

            runCatching { sector.addLot(overlapping) }

            assertEquals(listOf(firstLot), sector.lots)
        }
    }
}
