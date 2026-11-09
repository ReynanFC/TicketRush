package com.reynanfc.ticketrush.catalog.application.port.`in`.usecase

import com.reynanfc.ticketrush.catalog.application.port.`in`.command.AddLotCommand
import com.reynanfc.ticketrush.catalog.domain.model.Lot

interface AddLotUseCase {

    fun addLot(command: AddLotCommand): Lot
}
