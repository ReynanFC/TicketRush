package com.reynanfc.ticketrush.inventory.config

import com.reynanfc.ticketrush.inventory.application.port.`in`.usecase.ReserveTicketsUseCase
import com.reynanfc.ticketrush.inventory.application.port.out.LotStockRepository
import com.reynanfc.ticketrush.inventory.application.port.out.ReservationRepository
import com.reynanfc.ticketrush.inventory.application.service.ReserveTicketsService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock

// Wires the inventory use cases into Spring, outside the core, so the services stay free of annotations.
@Configuration
class InventoryConfiguration {

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun reserveTicketsService(
        lotStockRepository: LotStockRepository,
        reservationRepository: ReservationRepository,
        clock: Clock,
        transactionTemplate: TransactionTemplate,
    ): ReserveTicketsUseCase {
        val service = ReserveTicketsService(lotStockRepository, reservationRepository, clock)
        return TransactionalReserveTicketsUseCase(service, transactionTemplate)
    }
}
