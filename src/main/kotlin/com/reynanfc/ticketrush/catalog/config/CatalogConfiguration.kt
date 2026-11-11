package com.reynanfc.ticketrush.catalog.config

import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.AddLotUseCase
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.AddSectorUseCase
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.CreateEventUseCase
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.PublishEventUseCase
import com.reynanfc.ticketrush.catalog.application.port.out.EventRepository
import com.reynanfc.ticketrush.catalog.application.service.AddLotService
import com.reynanfc.ticketrush.catalog.application.service.AddSectorService
import com.reynanfc.ticketrush.catalog.application.service.CreateEventService
import com.reynanfc.ticketrush.catalog.application.service.PublishEventService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// Wires the catalog use cases into Spring, outside the core, so the services stay free of annotations.
@Configuration
class CatalogConfiguration {

    @Bean
    fun createEventService(eventRepository: EventRepository): CreateEventUseCase {
        return CreateEventService(eventRepository)
    }

    @Bean
    fun addSectorService(eventRepository: EventRepository): AddSectorUseCase {
        return AddSectorService(eventRepository)
    }

    @Bean
    fun addLotService(eventRepository: EventRepository): AddLotUseCase {
        return AddLotService(eventRepository)
    }

    @Bean
    fun publishEventService(eventRepository: EventRepository): PublishEventUseCase {
        return PublishEventService(eventRepository)
    }
}
