package com.reynanfc.ticketrush.inventory.config

import com.reynanfc.ticketrush.TestcontainersConfiguration
import com.reynanfc.ticketrush.inventory.adapter.out.persistence.repository.SpringDataLotStockRepository
import com.reynanfc.ticketrush.inventory.adapter.out.persistence.repository.SpringDataReservationRepository
import com.reynanfc.ticketrush.inventory.application.port.`in`.usecase.ReserveTicketsUseCase
import com.reynanfc.ticketrush.inventory.application.port.out.LotStockRepository
import com.reynanfc.ticketrush.inventory.fixtures.LotStockFixtures.LOT_ID
import com.reynanfc.ticketrush.inventory.fixtures.LotStockFixtures.aLotStock
import com.reynanfc.ticketrush.inventory.fixtures.ReserveTicketsCommandFixtures.aReserveTicketsCommand
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

// Checks the transaction boundary of the real bean, wired by InventoryConfiguration.
@SpringBootTest
@Import(TestcontainersConfiguration::class)
class TransactionalReserveTicketsUseCaseTest {

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
    fun `should commit the stock and the reservation together`() {
        lotStockRepository.save(aLotStock(total = 100))

        reserveTickets.reserve(aReserveTicketsCommand(quantity = 3))

        assertEquals(97, assertNotNull(lotStockRepository.findByLotId(LOT_ID)).available)
        assertEquals(1, springDataReservationRepository.count())
    }

    @Test
    fun `should roll back the stock when saving the reservation fails`() {
        lotStockRepository.save(aLotStock(total = 100))
        reserveTickets.reserve(aReserveTicketsCommand(quantity = 3))

        // Same order again: the stock is updated in memory, then the reservation hits UNIQUE (order_id).
        assertFailsWith<DataIntegrityViolationException> {
            reserveTickets.reserve(aReserveTicketsCommand(quantity = 3))
        }

        assertEquals(97, assertNotNull(lotStockRepository.findByLotId(LOT_ID)).available)
        assertEquals(1, springDataReservationRepository.count())
    }
}
