package com.reynanfc.ticketrush.inventory.domain.exception

import java.util.UUID

class LotStockNotFoundException(
    val lotId: UUID,
) : InventoryException("Stock for lot $lotId not found")
