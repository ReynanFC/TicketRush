package com.reynanfc.ticketrush.inventory

import com.reynanfc.ticketrush.TestcontainersConfiguration
import com.reynanfc.ticketrush.inventory.adapter.out.persistence.repository.SpringDataLotStockRepository
import com.reynanfc.ticketrush.inventory.adapter.out.persistence.repository.SpringDataReservationRepository
import com.reynanfc.ticketrush.inventory.application.port.`in`.usecase.ReserveTicketsUseCase
import com.reynanfc.ticketrush.inventory.application.port.out.LotStockRepository
import com.reynanfc.ticketrush.inventory.domain.exception.InsufficientStockException
import com.reynanfc.ticketrush.inventory.fixtures.LotStockFixtures.LOT_ID
import com.reynanfc.ticketrush.inventory.fixtures.LotStockFixtures.aLotStock
import com.reynanfc.ticketrush.inventory.fixtures.ReserveTicketsCommandFixtures.aReserveTicketsCommand
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Many buyers hit the same lot at the same time: the real bean (transaction included) against a real Postgres.
// No test transaction here on purpose: each thread must open its own, like real requests do (#15).
@SpringBootTest
@Import(TestcontainersConfiguration::class)
class ReserveTicketsConcurrencyTest {

    @Autowired
    private lateinit var reserveTickets: ReserveTicketsUseCase

    @Autowired
    private lateinit var lotStockRepository: LotStockRepository

    @Autowired
    private lateinit var springDataLotStockRepository: SpringDataLotStockRepository

    @Autowired
    private lateinit var springDataReservationRepository: SpringDataReservationRepository

    @AfterEach
    fun cleanUp() {
        springDataReservationRepository.deleteAll()
        springDataLotStockRepository.deleteAll()
    }

    @Test
    fun `should never reserve more tickets than the stock`() {

        lotStockRepository.save(aLotStock(total = 10))
        val buyers = 50
        val executor = Executors.newFixedThreadPool(10)
        val start = CountDownLatch(1)

        val success = AtomicInteger(0)
        val failure = AtomicInteger(0)
        val unexpected = AtomicInteger(0)

        repeat(buyers) {
            executor.submit {
                start.await()
                try {
                    reserveTickets.reserve(aReserveTicketsCommand(orderId = UUID.randomUUID(), quantity = 1))
                    success.incrementAndGet()
                } catch (e: InsufficientStockException) {
                    failure.incrementAndGet()
                } catch (e: Exception) {
                    unexpected.incrementAndGet()
                }
            }
        }
        start.countDown()
        executor.shutdown()
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS), "buyers did not finish in time")

        assertEquals(0, unexpected.get(), "unexpected errors")
        assertEquals(10, success.get(), "successful reservations")
        assertEquals(40, failure.get(), "rejected reservations")
        assertEquals(0, assertNotNull(lotStockRepository.findByLotId(LOT_ID)).available)
        assertEquals(10, springDataReservationRepository.count())
    }
}
