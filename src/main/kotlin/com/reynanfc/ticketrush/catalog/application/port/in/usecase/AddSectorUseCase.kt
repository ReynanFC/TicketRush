package com.reynanfc.ticketrush.catalog.application.port.`in`.usecase

import com.reynanfc.ticketrush.catalog.application.port.`in`.command.AddSectorCommand
import com.reynanfc.ticketrush.catalog.domain.model.Sector

interface AddSectorUseCase {

    fun addSector(command: AddSectorCommand): Sector
}