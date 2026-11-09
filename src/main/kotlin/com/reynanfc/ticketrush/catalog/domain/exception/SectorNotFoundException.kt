package com.reynanfc.ticketrush.catalog.domain.exception

import java.util.UUID

class SectorNotFoundException(
    val sectorId: UUID,
) : CatalogException("Sector $sectorId not found in the event")
