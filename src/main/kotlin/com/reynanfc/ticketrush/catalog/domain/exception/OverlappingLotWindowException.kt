package com.reynanfc.ticketrush.catalog.domain.exception

import com.reynanfc.ticketrush.catalog.domain.Lot
import java.util.UUID

class OverlappingLotWindowException(
    lot: Lot,
    conflictingLot: Lot,
) : CatalogException("Lot '${lot.name}' overlaps the sale window of lot '${conflictingLot.name}'") {
    val lotId: UUID = lot.id
    val conflictingLotId: UUID = conflictingLot.id
}
