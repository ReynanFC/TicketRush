package com.reynanfc.ticketrush.inventory.adapter.out.persistence.entity

import com.reynanfc.ticketrush.inventory.domain.enums.ReservationStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

// Database shape of the reservations table. lot_id is a plain column, not a @ManyToOne:
// LotStock and Reservation are loaded separately, never as one object graph.
@Entity
@Table(name = "reservations")
class ReservationEntity(
    @Id
    var id: UUID,

    @Column(name = "order_id", nullable = false)
    var orderId: UUID,

    @Column(name = "lot_id", nullable = false)
    var lotId: UUID,

    @Column(nullable = false)
    var quantity: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ReservationStatus,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,
)
