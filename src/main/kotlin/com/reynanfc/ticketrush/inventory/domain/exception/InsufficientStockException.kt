package com.reynanfc.ticketrush.inventory.domain.exception

import java.util.UUID

class InsufficientStockException(
    val lotId: UUID,
    val requested: Int,
    val available: Int,
) : InventoryException("Lot $lotId has $available tickets available, but $requested were requested")
