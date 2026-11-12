package com.reynanfc.ticketrush.inventory.config

import com.reynanfc.ticketrush.inventory.application.port.`in`.command.ReserveTicketsCommand
import com.reynanfc.ticketrush.inventory.application.port.`in`.usecase.ReserveTicketsUseCase
import com.reynanfc.ticketrush.inventory.domain.model.Reservation
import org.springframework.transaction.support.TransactionTemplate

// Decorator: runs the whole use case in one transaction without putting @Transactional in the core.
// Reading the stock, saving it and saving the reservation either all commit or all roll back.
class TransactionalReserveTicketsUseCase(
    private val delegate: ReserveTicketsUseCase,
    private val transactionTemplate: TransactionTemplate,
) : ReserveTicketsUseCase {

    override fun reserve(command: ReserveTicketsCommand): Reservation {
        return transactionTemplate.execute { delegate.reserve(command) }!!
    }
}
