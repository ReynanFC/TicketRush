package com.reynanfc.ticketrush.catalog.domain.model

import java.time.Instant
import java.util.UUID

class Lot (
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val priceCents: Long,
    var quantity: Int,
    val sortOrder: Int,
    val startsAt: Instant,
    val endsAt: Instant
) {
    init {
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(quantity > 0) { "Quantity must be positive" }
        require(priceCents >= 0) { "Price cent must be positive or zero" }
        require(endsAt.isAfter(startsAt)) { "EndsAt must be after startsAt" }
    }

    fun isOnSaleAt(moment: Instant): Boolean {
        return !moment.isBefore(startsAt) && moment.isBefore(endsAt)
    }

    fun overlaps(other: Lot): Boolean {
        return startsAt.isBefore(other.endsAt) && other.startsAt.isBefore(endsAt)
    }
}