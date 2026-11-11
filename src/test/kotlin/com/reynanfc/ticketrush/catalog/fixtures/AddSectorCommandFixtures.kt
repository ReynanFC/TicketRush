package com.reynanfc.ticketrush.catalog.fixtures

import com.reynanfc.ticketrush.catalog.application.port.`in`.command.AddSectorCommand
import java.util.UUID

// Fake data for tests: eventId is required because it must point to an event saved in the test.
object AddSectorCommandFixtures {

    fun anAddSectorCommand(
        eventId: UUID,
        name: String = "Pista",
    ) = AddSectorCommand(
        eventId = eventId,
        name = name,
    )
}
