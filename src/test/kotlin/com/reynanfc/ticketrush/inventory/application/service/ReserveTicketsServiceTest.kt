package com.reynanfc.ticketrush.inventory.application.service

import com.reynanfc.ticketrush.inventory.domain.enums.ReservationStatus
import com.reynanfc.ticketrush.inventory.domain.exception.InsufficientStockException
import com.reynanfc.ticketrush.inventory.domain.exception.LotStockNotFoundException
import com.reynanfc.ticketrush.inventory.fakes.InMemoryLotStockRepository
import com.reynanfc.ticketrush.inventory.fakes.InMemoryReservationRepository
import com.reynanfc.ticketrush.inventory.fixtures.LotStockFixtures.LOT_ID
import com.reynanfc.ticketrush.inventory.fixtures.LotStockFixtures.aLotStock
import com.reynanfc.ticketrush.inventory.fixtures.ReservationFixtures.ORDER_ID
import com.reynanfc.ticketrush.inventory.fixtures.ReserveTicketsCommandFixtures.aReserveTicketsCommand
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class ReserveTicketsServiceTest {

    private val now = Instant.parse("2026-12-05T18:00:00Z")

    // A new instance per test (JUnit lifecycle), so each test starts with empty repositories.
    private val lotStockRepository = InMemoryLotStockRepository()
    private val reservationRepository = InMemoryReservationRepository()
    private val service = ReserveTicketsService(
        lotStockRepository,
        reservationRepository,
        Clock.fixed(now, ZoneOffset.UTC),
    )

    @Test
    fun `should take the tickets out of the saved stock`() {
        lotStockRepository.save(aLotStock(total = 100))

        service.reserve(aReserveTicketsCommand(quantity = 3))

        val stock = assertNotNull(lotStockRepository.findByLotId(LOT_ID))
        assertEquals(97, stock.available)
    }

    @Test
    fun `should save a pending reservation for the order`() {
        lotStockRepository.save(aLotStock())

        val reservation = service.reserve(aReserveTicketsCommand(quantity = 3))

        val saved = assertNotNull(reservationRepository.findById(reservation.id))
        assertEquals(ReservationStatus.PENDING, saved.status)
        assertEquals(ORDER_ID, saved.orderId)
        assertEquals(LOT_ID, saved.lotId)
        assertEquals(3, saved.quantity)
    }

    @Test
    fun `should hold the tickets for the reservation TTL`() {
        lotStockRepository.save(aLotStock())

        val reservation = service.reserve(aReserveTicketsCommand())

        assertEquals(now.plus(ReserveTicketsService.RESERVATION_TTL), reservation.expiresAt)
    }

    @Test
    fun `should reject a lot without stock`() {
        val missingLotId = UUID.randomUUID()

        val exception = assertFailsWith<LotStockNotFoundException> {
            service.reserve(aReserveTicketsCommand(lotId = missingLotId))
        }

        assertEquals(missingLotId, exception.lotId)
        assertEquals(0, reservationRepository.count())
    }

    @Test
    fun `should save nothing when there are not enough tickets`() {
        lotStockRepository.save(aLotStock(total = 100, available = 2))

        assertFailsWith<InsufficientStockException> {
            service.reserve(aReserveTicketsCommand(quantity = 3))
        }

        assertEquals(2, assertNotNull(lotStockRepository.findByLotId(LOT_ID)).available)
        assertEquals(0, reservationRepository.count())
    }
}
