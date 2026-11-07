package com.reynanfc.ticketrush.catalog.fixtures

import com.reynanfc.ticketrush.catalog.domain.Lot
import com.reynanfc.ticketrush.catalog.domain.Sector

// Fake data for tests: an empty, valid sector by default.
object SectorFixtures {

    fun aSector(
        name: String = "Pista",
        lots: List<Lot> = emptyList(),
    ) = Sector(
        name = name,
    ).apply { lots.forEach { addLot(it) } }
}
