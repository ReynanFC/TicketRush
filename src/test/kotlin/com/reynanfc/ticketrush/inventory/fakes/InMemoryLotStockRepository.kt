package com.reynanfc.ticketrush.inventory.fakes

import com.reynanfc.ticketrush.inventory.application.port.out.LotStockRepository
import com.reynanfc.ticketrush.inventory.domain.model.LotStock
import java.util.UUID

// Fake adapter for the LotStockRepository port: keeps stocks in memory, no database.
// Stores a copy, so a test only sees a change after save() (the issue #8 lesson).
class InMemoryLotStockRepository : LotStockRepository {

    private val stocks = mutableMapOf<UUID, LotStock>()

    override fun save(stock: LotStock) {
        stocks[stock.lotId] = stock.copy()
    }

    override fun findByLotId(lotId: UUID): LotStock? = stocks[lotId]?.copy()

    private fun LotStock.copy() = LotStock.restore(lotId, sectorId, total, available)
}
