package com.reynanfc.ticketrush.inventory.application.port.`in`.command

import java.util.UUID

data class ReserveTicketsCommand(
    val orderId: UUID,
    val lotId: UUID,
    val quantity: Int,
)
