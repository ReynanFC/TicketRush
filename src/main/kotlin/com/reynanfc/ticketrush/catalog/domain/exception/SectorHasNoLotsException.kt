package com.reynanfc.ticketrush.catalog.domain.exception

import java.util.UUID

class SectorHasNoLotsException(
    val sectorId: UUID,
    val sectorName: String,
) : CatalogException("Sector '$sectorName' cannot be published without lots")
