package com.reynanfc.ticketrush.catalog.domain.exception

class DuplicateSectorNameException(
    val sectorName: String,
) : CatalogException("A sector named '$sectorName' already exists in the event")
