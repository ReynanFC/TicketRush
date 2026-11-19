package com.reynanfc.ticketrush.inventory.application.port.out

import com.reynanfc.ticketrush.inventory.domain.model.LotStock
import java.util.UUID

interface LotStockRepository {

    fun save(stock: LotStock)
    fun findByLotId(lotId: UUID): LotStock?
    fun findByLotIdForReservation(lotId: UUID): LotStock?
}
