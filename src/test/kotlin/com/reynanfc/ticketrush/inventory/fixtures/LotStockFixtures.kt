package com.reynanfc.ticketrush.inventory.fixtures

import com.reynanfc.ticketrush.inventory.domain.model.LotStock
import java.util.UUID

// Fake data for tests: valid by default; each test overrides only what matters.
object LotStockFixtures {

    val LOT_ID: UUID = UUID.fromString("00000000-0000-0000-0000-00000000a001")
    val SECTOR_ID: UUID = UUID.fromString("00000000-0000-0000-0000-00000000b001")

    fun aLotStock(
        lotId: UUID = LOT_ID,
        sectorId: UUID = SECTOR_ID,
        total: Int = 100,
        available: Int = total,
    ) = LotStock.restore(
        lotId = lotId,
        sectorId = sectorId,
        total = total,
        available = available,
    )
}
