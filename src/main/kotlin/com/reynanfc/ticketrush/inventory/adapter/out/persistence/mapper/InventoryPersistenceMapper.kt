package com.reynanfc.ticketrush.inventory.adapter.out.persistence.mapper

import com.reynanfc.ticketrush.inventory.adapter.out.persistence.entity.LotStockEntity
import com.reynanfc.ticketrush.inventory.adapter.out.persistence.entity.ReservationEntity
import com.reynanfc.ticketrush.inventory.domain.model.LotStock
import com.reynanfc.ticketrush.inventory.domain.model.Reservation

// Translates the inventory domain to its database shape and back.
// Going back uses restore(), so the saved `available` and `status` are kept as they are.
object InventoryPersistenceMapper {

    fun toEntity(stock: LotStock) = LotStockEntity(
        lotId = stock.lotId,
        sectorId = stock.sectorId,
        total = stock.total,
        available = stock.available,
    )

    fun toDomain(entity: LotStockEntity) = LotStock.restore(
        lotId = entity.lotId,
        sectorId = entity.sectorId,
        total = entity.total,
        available = entity.available,
    )

    fun toEntity(reservation: Reservation) = ReservationEntity(
        id = reservation.id,
        orderId = reservation.orderId,
        lotId = reservation.lotId,
        quantity = reservation.quantity,
        status = reservation.status,
        expiresAt = reservation.expiresAt,
    )

    fun toDomain(entity: ReservationEntity) = Reservation.restore(
        id = entity.id,
        orderId = entity.orderId,
        lotId = entity.lotId,
        quantity = entity.quantity,
        expiresAt = entity.expiresAt,
        status = entity.status,
    )
}
