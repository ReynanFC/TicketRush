package com.reynanfc.ticketrush

import com.reynanfc.ticketrush.catalog.application.port.`in`.command.PublishEventCommand
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.AddLotUseCase
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.AddSectorUseCase
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.CreateEventUseCase
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.PublishEventUseCase
import com.reynanfc.ticketrush.catalog.domain.enums.EventStatus
import com.reynanfc.ticketrush.catalog.fixtures.AddLotCommandFixtures.anAddLotCommand
import com.reynanfc.ticketrush.catalog.fixtures.AddSectorCommandFixtures.anAddSectorCommand
import com.reynanfc.ticketrush.catalog.fixtures.CreateEventCommandFixtures.aCreateEventCommand
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals

@Import(TestcontainersConfiguration::class)
@SpringBootTest
class TicketRushApplicationTests {

    @Autowired
    private lateinit var createEvent: CreateEventUseCase

    @Autowired
    private lateinit var addSector: AddSectorUseCase

    @Autowired
    private lateinit var addLot: AddLotUseCase

    @Autowired
    private lateinit var publishEvent: PublishEventUseCase

    @Test
    fun contextLoads() {
    }

    @Test
    fun `should wire the catalog use cases`() {
        val event = createEvent.create(aCreateEventCommand())
        val sector = addSector.addSector(anAddSectorCommand(eventId = event.id))
        addLot.addLot(anAddLotCommand(eventId = event.id, sectorId = sector.id))

        val published = publishEvent.publish(PublishEventCommand(eventId = event.id))

        assertEquals(EventStatus.PUBLISHED, published.status)
    }
}
