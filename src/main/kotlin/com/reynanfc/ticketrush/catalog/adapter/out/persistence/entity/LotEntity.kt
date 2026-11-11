package com.reynanfc.ticketrush.catalog.adapter.out.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

// Database shape of the lots table. The sector_id column is managed by SectorEntity.lots.
@Entity
@Table(name = "lots")
class LotEntity(
    @Id
    var id: UUID,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(name = "price_cents", nullable = false)
    var priceCents: Long,

    @Column(nullable = false)
    var quantity: Int,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int,

    @Column(name = "starts_at", nullable = false)
    var startsAt: Instant,

    @Column(name = "ends_at", nullable = false)
    var endsAt: Instant,
)
