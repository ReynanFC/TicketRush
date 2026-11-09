package com.reynanfc.ticketrush.catalog.adapter.out.persistence.entity

import com.reynanfc.ticketrush.catalog.domain.enums.EventStatus
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

// Database shape of the events table. No business rules here: they live in domain.model.Event.
@Entity
@Table(name = "events")
class EventEntity(
    @Id
    var id: UUID,

    @Column(name = "organizer_id", nullable = false)
    var organizerId: UUID,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(nullable = false, length = 200)
    var venue: String,

    @Column(nullable = false, length = 100)
    var city: String,

    @Column(name = "starts_at", nullable = false)
    var startsAt: Instant,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: EventStatus,

    // Unidirectional: the sector has no reference back to the event, only the event_id column.
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "event_id", nullable = false)
    var sectors: MutableList<SectorEntity> = mutableListOf(),
)
