package com.reynanfc.ticketrush.inventory.application.port.`in`.usecase

import com.reynanfc.ticketrush.inventory.application.port.`in`.command.ReserveTicketsCommand
import com.reynanfc.ticketrush.inventory.domain.model.Reservation

interface ReserveTicketsUseCase {

    fun reserve(command: ReserveTicketsCommand): Reservation
}
