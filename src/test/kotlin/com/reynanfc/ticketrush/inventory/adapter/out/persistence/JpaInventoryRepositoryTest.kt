package com.reynanfc.ticketrush.inventory.adapter.out.persistence

import com.reynanfc.ticketrush.TestcontainersConfiguration
import com.reynanfc.ticketrush.inventory.adapter.out.persistence.repository.SpringDataLotStockRepository
import com.reynanfc.ticketrush.inventory.adapter.out.persistence.repository.SpringDataReservationRepository
import com.reynanfc.ticketrush.inventory.domain.enums.ReservationStatus
import com.reynanfc.ticketrush.inventory.fixtures.LotStockFixtures.LOT_ID
import com.reynanfc.ticketrush.inventory.fixtures.LotStockFixtures.SECTOR_ID
import com.reynanfc.ticketrush.inventory.fixtures.LotStockFixtures.aLotStock
import com.reynanfc.ticketrush.inventory.fixtures.ReservationFixtures.EXPIRES_AT
import com.reynanfc.ticketrush.inventory.fixtures.ReservationFixtures.ORDER_ID
import com.reynanfc.ticketrush.inventory.fixtures.ReservationFixtures.aReservation
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

// Integration test against a real Postgres (Testcontainers) with the V2 schema applied by Flyway.
// Same setup as JpaEventRepositoryTest: no test transaction, so every call hits the database.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration::class, JpaLotStockRepository::class, JpaReservationRepository::class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JpaInventoryRepositoryTest {

    @Autowired
    private lateinit var lotStockRepository: JpaLotStockRepository

    @Autowired
    private lateinit var reservationRepository: JpaReservationRepository

    @Autowired
    private lateinit var springDataLotStockRepository: SpringDataLotStockRepository

    @Autowired
    private lateinit var springDataReservationRepository: SpringDataReservationRepository

    // No rollback without a test transaction, so clean up by hand. Reservations first, because of the FK.
    @AfterEach
    fun cleanUp() {
        springDataReservationRepository.deleteAll()
        springDataLotStockRepository.deleteAll()
    }

    @Nested
    inner class LotStocks {

        @Test
        fun `should save and find a stock`() {
            lotStockRepository.save(aLotStock(total = 100, available = 40))

            val found = assertNotNull(lotStockRepository.findByLotId(LOT_ID))

            assertEquals(LOT_ID, found.lotId)
            assertEquals(SECTOR_ID, found.sectorId)
            assertEquals(100, found.total)
            assertEquals(40, found.available)
        }

        @Test
        fun `should update the available tickets`() {
            val stock = aLotStock(total = 100).also { lotStockRepository.save(it) }

            stock.reserve(ORDER_ID, quantity = 3, expiresAt = EXPIRES_AT)
            lotStockRepository.save(stock)

            assertEquals(97, assertNotNull(lotStockRepository.findByLotId(LOT_ID)).available)
        }

        @Test
        fun `should return null when the lot has no stock`() {
            assertNull(lotStockRepository.findByLotId(UUID.randomUUID()))
        }
    }

    @Nested
    inner class Reservations {

        @Test
        fun `should save and find a reservation`() {
            lotStockRepository.save(aLotStock())
            val reservation = aReservation(quantity = 3)

            reservationRepository.save(reservation)
            val found = assertNotNull(reservationRepository.findById(reservation.id))

            assertEquals(ORDER_ID, found.orderId)
            assertEquals(LOT_ID, found.lotId)
            assertEquals(3, found.quantity)
            assertEquals(EXPIRES_AT, found.expiresAt)
            assertEquals(ReservationStatus.PENDING, found.status)
        }

        @Test
        fun `should update the status`() {
            lotStockRepository.save(aLotStock())
            val reservation = aReservation().also { reservationRepository.save(it) }

            reservation.confirm()
            reservationRepository.save(reservation)

            assertEquals(ReservationStatus.CONFIRMED, assertNotNull(reservationRepository.findById(reservation.id)).status)
        }

        @Test
        fun `should reject a reservation for a lot without stock`() {
            assertFailsWith<DataIntegrityViolationException> {
                reservationRepository.save(aReservation(lotId = UUID.randomUUID()))
            }
        }

        @Test
        fun `should reject a second reservation for the same order`() {
            lotStockRepository.save(aLotStock())
            reservationRepository.save(aReservation(orderId = ORDER_ID))

            assertFailsWith<DataIntegrityViolationException> {
                reservationRepository.save(aReservation(orderId = ORDER_ID))
            }
        }
    }
}
