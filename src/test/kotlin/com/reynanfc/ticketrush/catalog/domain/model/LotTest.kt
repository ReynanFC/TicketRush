package com.reynanfc.ticketrush.catalog.domain.model

import com.reynanfc.ticketrush.catalog.fixtures.LotFixtures.LOT_ENDS_AT
import com.reynanfc.ticketrush.catalog.fixtures.LotFixtures.LOT_STARTS_AT
import com.reynanfc.ticketrush.catalog.fixtures.LotFixtures.aLot
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LotTest {

    @Nested
    inner class IsOnSaleAt {

        @Test
        fun `should not be on sale before startsAt`() {
            val lot = aLot(startsAt = LOT_STARTS_AT)

            val onSale = lot.isOnSaleAt(LOT_STARTS_AT.minusSeconds(1))

            assertFalse(onSale)
        }

        @Test
        fun `should be on sale exactly at startsAt`() {
            val lot = aLot(startsAt = LOT_STARTS_AT)

            val onSale = lot.isOnSaleAt(LOT_STARTS_AT)

            assertTrue(onSale)
        }

        @Test
        fun `should be on sale in the middle of the window`() {
            val lot = aLot(startsAt = LOT_STARTS_AT, endsAt = LOT_ENDS_AT)
            val middle = LOT_STARTS_AT.plus(Duration.between(LOT_STARTS_AT, LOT_ENDS_AT).dividedBy(2))

            val onSale = lot.isOnSaleAt(middle)

            assertTrue(onSale)
        }

        @Test
        fun `should not be on sale exactly at endsAt`() {
            val lot = aLot(endsAt = LOT_ENDS_AT)

            val onSale = lot.isOnSaleAt(LOT_ENDS_AT)

            assertFalse(onSale)
        }

        @Test
        fun `should not be on sale after endsAt`() {
            val lot = aLot(endsAt = LOT_ENDS_AT)

            val onSale = lot.isOnSaleAt(LOT_ENDS_AT.plusSeconds(1))

            assertFalse(onSale)
        }
    }

    @Nested
    inner class Overlaps {

        private val lot = aLot(startsAt = LOT_STARTS_AT, endsAt = LOT_ENDS_AT)

        @Test
        fun `should not overlap a lot that starts exactly when it ends`() {
            val next = aLot(startsAt = LOT_ENDS_AT, endsAt = LOT_ENDS_AT.plus(Duration.ofDays(10)))

            assertFalse(lot.overlaps(next))
        }

        @Test
        fun `should overlap a lot entirely inside its window`() {
            val inside = aLot(startsAt = LOT_STARTS_AT.plus(Duration.ofDays(1)), endsAt = LOT_ENDS_AT.minus(Duration.ofDays(1)))

            assertTrue(lot.overlaps(inside))
        }

        @Test
        fun `should be symmetric`() {
            val partial = aLot(startsAt = LOT_ENDS_AT.minusSeconds(1), endsAt = LOT_ENDS_AT.plus(Duration.ofDays(10)))

            assertEquals(lot.overlaps(partial), partial.overlaps(lot))
        }
    }

    @Nested
    inner class Creation {

        @Test
        fun `should reject blank name`() {
            assertFailsWith<IllegalArgumentException> { aLot(name = "  ") }
        }

        @ParameterizedTest
        @ValueSource(ints = [0, -1])
        fun `should reject zero or negative quantity`(quantity: Int) {
            assertFailsWith<IllegalArgumentException> { aLot(quantity = quantity) }
        }

        @Test
        fun `should reject negative price`() {
            assertFailsWith<IllegalArgumentException> { aLot(priceCents = -1) }
        }

        @Test
        fun `should accept free lot`() {
            val lot = aLot(priceCents = 0)

            assertEquals(0L, lot.priceCents)
        }

        @Test
        fun `should reject endsAt equal to startsAt`() {
            assertFailsWith<IllegalArgumentException> {
                aLot(startsAt = LOT_STARTS_AT, endsAt = LOT_STARTS_AT)
            }
        }
    }
}
