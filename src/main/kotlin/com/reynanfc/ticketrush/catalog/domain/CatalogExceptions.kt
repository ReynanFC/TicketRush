package com.reynanfc.ticketrush.catalog.domain

import java.util.UUID

// Base type for business rule violations in the catalog.
// Sealed: every subtype lives in this file, so a `when` over them is exhaustive.
sealed class CatalogException(message: String) : RuntimeException(message)

class DuplicateLotSortOrderException(
    val sortOrder: Int,
) : CatalogException("A lot with sort order $sortOrder already exists in the sector")

class OverlappingLotWindowException(
    lot: Lot,
    conflictingLot: Lot,
) : CatalogException("Lot '${lot.name}' overlaps the sale window of lot '${conflictingLot.name}'") {
    val lotId: UUID = lot.id
    val conflictingLotId: UUID = conflictingLot.id
}
