package com.reynanfc.ticketrush.catalog.domain.exception

class DuplicateLotSortOrderException(
    val sortOrder: Int,
) : CatalogException("A lot with sort order $sortOrder already exists in the sector")
