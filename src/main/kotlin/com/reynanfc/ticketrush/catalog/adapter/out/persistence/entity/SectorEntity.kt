package com.reynanfc.ticketrush.catalog.adapter.out.persistence.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.util.UUID

// Database shape of the sectors table. The event_id column is managed by EventEntity.sectors.
@Entity
@Table(name = "sectors")
class SectorEntity(
    @Id
    var id: UUID,

    @Column(nullable = false, length = 100)
    var name: String,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "sector_id", nullable = false)
    var lots: MutableList<LotEntity> = mutableListOf(),
)
