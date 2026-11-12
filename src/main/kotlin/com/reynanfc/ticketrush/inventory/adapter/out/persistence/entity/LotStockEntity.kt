package com.reynanfc.ticketrush.inventory.adapter.out.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

// Database shape of the lot_stock table. No business rules here: they live in domain.model.LotStock.
@Entity
@Table(name = "lot_stock")
class LotStockEntity(
    @Id
    @Column(name = "lot_id")
    var lotId: UUID,

    @Column(name = "sector_id", nullable = false)
    var sectorId: UUID,

    @Column(nullable = false)
    var total: Int,

    @Column(nullable = false)
    var available: Int,
)
