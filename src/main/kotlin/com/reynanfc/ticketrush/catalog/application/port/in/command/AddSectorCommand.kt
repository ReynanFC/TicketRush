package com.reynanfc.ticketrush.catalog.application.port.`in`.command

import java.util.UUID

data class AddSectorCommand(val eventId: UUID, val name: String)
