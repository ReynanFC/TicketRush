package com.reynanfc.ticketrush.inventory.domain.model

import com.reynanfc.ticketrush.inventory.domain.enums.ReservationStatus
import com.reynanfc.ticketrush.inventory.domain.exception.InsufficientStockException
import com.reynanfc.ticketrush.inventory.domain.exception.InvalidReservationTransitionException
import com.reynanfc.ticketrush.inventory.fixtures.LotStockFixtures.LOT_ID
import com.reynanfc.ticketrush.inventory.fixtures.LotStockFixtures.SECTOR_ID
import com.reynanfc.ticketrush.inventory.fixtures.LotStockFixtures.aLotStock
import com.reynanfc.ticketrush.inventory.fixtures.ReservationFixtures.EXPIRES_AT
import com.reynanfc.ticketrush.inventory.fixtures.ReservationFixtures.ORDER_ID
import com.reynanfc.ticketrush.inventory.fixtures.ReservationFixtures.aReservation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LotStockTest {

    @Nested
    inner class Creation {

        @ParameterizedTest
        @ValueSource(ints = [0, -1])
        fun `should reject a total that is not positive`(total: Int) {
            assertFailsWith<IllegalArgumentException> {
                LotStock(lotId = LOT_ID, sectorId = SECTOR_ID, total = total)
            }
        }

        @Test
        fun `should start with every ticket available`() {
            val stock = LotStock(lotId = LOT_ID, sectorId = SECTOR_ID, total = 100)

            assertEquals(100, stock.available)
        }
    }

    @Nested
    inner class Restore {

        @Test
        fun `should keep how many tickets are left`() {
            val stock = aLotStock(total = 100, available = 30)

            assertEquals(100, stock.total)
            assertEquals(30, stock.available)
        }

        @ParameterizedTest
        @ValueSource(ints = [-1, 101])
        fun `should reject available outside 0 and total`(available: Int) {
            assertFailsWith<IllegalArgumentException> { aLotStock(total = 100, available = available) }
        }
    }

    @Nested
    inner class Reserve {

        @Test
        fun `should take the tickets out of the stock`() {
            val stock = aLotStock(total = 100)

            stock.reserve(ORDER_ID, quantity = 3, expiresAt = EXPIRES_AT)

            assertEquals(97, stock.available)
        }

        @Test
        fun `should return a pending reservation for the order`() {
            val stock = aLotStock()

            val reservation = stock.reserve(ORDER_ID, quantity = 3, expiresAt = EXPIRES_AT)

            assertEquals(ReservationStatus.PENDING, reservation.status)
            assertEquals(ORDER_ID, reservation.orderId)
            assertEquals(LOT_ID, reservation.lotId)
            assertEquals(3, reservation.quantity)
            assertEquals(EXPIRES_AT, reservation.expiresAt)
        }

        @Test
        fun `should allow reserving the last tickets`() {
            val stock = aLotStock(total = 100, available = 2)

            stock.reserve(ORDER_ID, quantity = 2, expiresAt = EXPIRES_AT)

            assertEquals(0, stock.available)
        }

        @Test
        fun `should reject more tickets than available and keep the stock`() {
            val stock = aLotStock(total = 100, available = 2)

            val exception = assertFailsWith<InsufficientStockException> {
                stock.reserve(ORDER_ID, quantity = 3, expiresAt = EXPIRES_AT)
            }

            assertEquals(3, exception.requested)
            assertEquals(2, exception.available)
            assertEquals(2, stock.available)
        }

        @Test
        fun `should reject any reservation when sold out`() {
            val stock = aLotStock(total = 100, available = 0)

            assertFailsWith<InsufficientStockException> {
                stock.reserve(ORDER_ID, quantity = 1, expiresAt = EXPIRES_AT)
            }
        }

        @ParameterizedTest
        @ValueSource(ints = [0, -1])
        fun `should reject a quantity that is not positive`(quantity: Int) {
            val stock = aLotStock()

            assertFailsWith<IllegalArgumentException> {
                stock.reserve(ORDER_ID, quantity = quantity, expiresAt = EXPIRES_AT)
            }
            assertEquals(100, stock.available)
        }
    }

    @Nested
    inner class Release {

        @Test
        fun `should give the tickets back to the stock`() {
            val stock = aLotStock(total = 100)
            val reservation = stock.reserve(ORDER_ID, quantity = 3, expiresAt = EXPIRES_AT)

            stock.release(reservation)

            assertEquals(100, stock.available)
            assertEquals(ReservationStatus.RELEASED, reservation.status)
        }

        @Test
        fun `should give the tickets back only once when released twice`() {
            val stock = aLotStock(total = 100)
            val reservation = stock.reserve(ORDER_ID, quantity = 3, expiresAt = EXPIRES_AT)

            stock.release(reservation)
            stock.release(reservation)

            assertEquals(100, stock.available)
        }

        @Test
        fun `should not give back the tickets of a confirmed reservation`() {
            val stock = aLotStock(total = 100, available = 97)
            val reservation = aReservation(quantity = 3, status = ReservationStatus.CONFIRMED)

            assertFailsWith<InvalidReservationTransitionException> { stock.release(reservation) }
            assertEquals(97, stock.available)
        }

        @Test
        fun `should not give back the tickets of an expired reservation`() {
            val stock = aLotStock(total = 100, available = 100)
            val reservation = aReservation(quantity = 3, status = ReservationStatus.EXPIRED)

            stock.release(reservation)

            assertEquals(100, stock.available)
        }

        @Test
        fun `should reject a reservation from another lot`() {
            val stock = aLotStock(total = 100, available = 97)
            val reservation = aReservation(lotId = UUID.randomUUID(), quantity = 3)

            assertFailsWith<IllegalArgumentException> { stock.release(reservation) }
            assertEquals(97, stock.available)
        }
    }
}
