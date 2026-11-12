package com.reynanfc.ticketrush.inventory.domain.model

import com.reynanfc.ticketrush.inventory.domain.enums.ReservationStatus
import com.reynanfc.ticketrush.inventory.domain.enums.ReservationStatus.CONFIRMED
import com.reynanfc.ticketrush.inventory.domain.enums.ReservationStatus.EXPIRED
import com.reynanfc.ticketrush.inventory.domain.enums.ReservationStatus.PENDING
import com.reynanfc.ticketrush.inventory.domain.enums.ReservationStatus.RELEASED
import com.reynanfc.ticketrush.inventory.domain.exception.InvalidReservationTransitionException
import com.reynanfc.ticketrush.inventory.fixtures.LotStockFixtures.LOT_ID
import com.reynanfc.ticketrush.inventory.fixtures.ReservationFixtures.EXPIRES_AT
import com.reynanfc.ticketrush.inventory.fixtures.ReservationFixtures.ORDER_ID
import com.reynanfc.ticketrush.inventory.fixtures.ReservationFixtures.aReservation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReservationTest {

    @Nested
    inner class Creation {

        @ParameterizedTest
        @ValueSource(ints = [0, -1])
        fun `should reject a quantity that is not positive`(quantity: Int) {
            assertFailsWith<IllegalArgumentException> {
                Reservation(orderId = ORDER_ID, lotId = LOT_ID, quantity = quantity, expiresAt = EXPIRES_AT)
            }
        }

        @Test
        fun `should start as pending`() {
            val reservation = Reservation(orderId = ORDER_ID, lotId = LOT_ID, quantity = 2, expiresAt = EXPIRES_AT)

            assertEquals(PENDING, reservation.status)
        }

        @ParameterizedTest
        @EnumSource(ReservationStatus::class)
        fun `should restore any status`(status: ReservationStatus) {
            assertEquals(status, aReservation(status = status).status)
        }
    }

    @Nested
    inner class Confirm {

        @Test
        fun `should confirm a pending reservation`() {
            val reservation = aReservation(status = PENDING)

            reservation.confirm()

            assertEquals(CONFIRMED, reservation.status)
        }

        @Test
        fun `should do nothing when already confirmed`() {
            val reservation = aReservation(status = CONFIRMED)

            reservation.confirm()

            assertEquals(CONFIRMED, reservation.status)
        }

        @ParameterizedTest
        @EnumSource(names = ["EXPIRED", "RELEASED"])
        fun `should reject confirming a reservation that is over`(status: ReservationStatus) {
            val reservation = aReservation(status = status)

            val exception = assertFailsWith<InvalidReservationTransitionException> { reservation.confirm() }

            assertEquals(status, exception.from)
            assertEquals(CONFIRMED, exception.to)
            assertEquals(status, reservation.status)
        }
    }

    @Nested
    inner class Expire {

        @Test
        fun `should expire a pending reservation and ask for the tickets back`() {
            val reservation = aReservation(status = PENDING)

            assertTrue(reservation.expire())
            assertEquals(EXPIRED, reservation.status)
        }

        @ParameterizedTest
        @EnumSource(names = ["CONFIRMED", "EXPIRED", "RELEASED"])
        fun `should leave any other status untouched`(status: ReservationStatus) {
            val reservation = aReservation(status = status)

            assertFalse(reservation.expire())
            assertEquals(status, reservation.status)
        }
    }

    @Nested
    inner class Release {

        @Test
        fun `should release a pending reservation and ask for the tickets back`() {
            val reservation = aReservation(status = PENDING)

            assertTrue(reservation.release())
            assertEquals(RELEASED, reservation.status)
        }

        @ParameterizedTest
        @EnumSource(names = ["EXPIRED", "RELEASED"])
        fun `should leave a reservation that is already over untouched`(status: ReservationStatus) {
            val reservation = aReservation(status = status)

            assertFalse(reservation.release())
            assertEquals(status, reservation.status)
        }

        @Test
        fun `should reject releasing a confirmed reservation`() {
            val reservation = aReservation(status = CONFIRMED)

            val exception = assertFailsWith<InvalidReservationTransitionException> { reservation.release() }

            assertEquals(CONFIRMED, exception.from)
            assertEquals(RELEASED, exception.to)
            assertEquals(CONFIRMED, reservation.status)
        }
    }
}
