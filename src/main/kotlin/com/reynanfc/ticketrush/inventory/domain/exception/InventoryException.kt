package com.reynanfc.ticketrush.inventory.domain.exception

// Base type for business rule violations in the inventory.
// Sealed: every subtype lives in this package, so a `when` over them is exhaustive.
sealed class InventoryException(message: String) : RuntimeException(message)
