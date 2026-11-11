package com.reynanfc.ticketrush.catalog.domain.exception

// Base type for business rule violations in the catalog.
// Sealed: every subtype lives in this package, so a `when` over them is exhaustive.
sealed class CatalogException(message: String) : RuntimeException(message)
