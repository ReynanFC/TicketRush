package com.reynanfc.ticketrush.catalog.application.port.`in`.usecase

import com.reynanfc.ticketrush.catalog.application.port.`in`.command.PublishEventCommand
import com.reynanfc.ticketrush.catalog.domain.model.Event

interface PublishEventUseCase {

    fun publish(command: PublishEventCommand): Event
}
