package com.reynanfc.ticketrush.catalog.adapter.`in`.web

import com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.request.AddLotRequestDTO
import com.reynanfc.ticketrush.catalog.application.port.`in`.command.PublishEventCommand
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.AddLotUseCase
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.AddSectorUseCase
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.CreateEventUseCase
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.PublishEventUseCase
import com.reynanfc.ticketrush.catalog.domain.exception.DuplicateSectorNameException
import com.reynanfc.ticketrush.catalog.domain.exception.EventHasNoSectorsException
import com.reynanfc.ticketrush.catalog.domain.exception.EventNotFoundException
import com.reynanfc.ticketrush.catalog.domain.exception.OverlappingLotWindowException
import com.reynanfc.ticketrush.catalog.domain.exception.SectorNotFoundException
import com.reynanfc.ticketrush.catalog.fixtures.AddLotCommandFixtures.anAddLotCommand
import com.reynanfc.ticketrush.catalog.fixtures.AddSectorCommandFixtures.anAddSectorCommand
import com.reynanfc.ticketrush.catalog.fixtures.CreateEventCommandFixtures.aCreateEventCommand
import com.reynanfc.ticketrush.catalog.fixtures.EventFixtures.anEvent
import com.reynanfc.ticketrush.catalog.fixtures.LotFixtures.aLot
import com.reynanfc.ticketrush.catalog.fixtures.SectorFixtures.aSector
import com.reynanfc.ticketrush.catalog.fixtures.web.AddLotRequestDTOFixtures.REQUEST_LOT_ENDS_AT
import com.reynanfc.ticketrush.catalog.fixtures.web.AddLotRequestDTOFixtures.REQUEST_LOT_STARTS_AT
import com.reynanfc.ticketrush.catalog.fixtures.web.AddLotRequestDTOFixtures.anAddLotRequest
import com.reynanfc.ticketrush.catalog.fixtures.web.AddSectorRequestDTOFixtures.anAddSectorRequest
import com.reynanfc.ticketrush.catalog.fixtures.web.CreateEventRequestDTOFixtures.REQUEST_EVENT_STARTS_AT
import com.reynanfc.ticketrush.catalog.fixtures.web.CreateEventRequestDTOFixtures.aCreateEventRequest
import org.junit.jupiter.api.Nested
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.util.UUID
import kotlin.test.Test

// Web slice: only EventController, CatalogExceptionHandler and Jackson/validation are loaded. No database.
// The use cases are Mockito mocks, so the domain rules do NOT run here: a test that expects a business
// error must make the mock throw it. What is tested is the HTTP translation (status, body, headers).
@WebMvcTest(EventController::class)
class EventControllerTest {

    companion object {
        private const val BASE_URL = "http://localhost/events"
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    // Turns request DTOs (see fixtures/web) into JSON for the request body.
    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @MockitoBean
    private lateinit var createEventUseCase: CreateEventUseCase

    @MockitoBean
    private lateinit var addSectorUseCase: AddSectorUseCase

    @MockitoBean
    private lateinit var addLotUseCase: AddLotUseCase

    @MockitoBean
    private lateinit var publishEventUseCase: PublishEventUseCase

    @Nested
    inner class CreateEvent {

        @Test
        fun `should return 201 with the Location header and the created event`() {

            val request = aCreateEventRequest()
            val expectedCommand = aCreateEventCommand(startsAt = REQUEST_EVENT_STARTS_AT)
            val event = anEvent(startsAt = REQUEST_EVENT_STARTS_AT)

            given(createEventUseCase.create(expectedCommand)).willReturn(event)

            mockMvc.post("/events") {
                contentType = MediaType.APPLICATION_JSON
                content = jsonMapper.writeValueAsString(request)
            }
                .andExpect {
                    status { isCreated() }
                    header { string(HttpHeaders.LOCATION, "$BASE_URL/${event.id}") }
                    jsonPath("$.id") { value(event.id.toString()) }
                    jsonPath("$.name") { value("Festival de Verão") }
                    jsonPath("$.startsAt") { value("2030-03-20T23:00:00Z") }
                    jsonPath("$.status") { value("DRAFT") }
                    jsonPath("$.sectors") { isEmpty() }
                }

            then(createEventUseCase).should().create(expectedCommand)
        }

        @Test
        fun `should return 400 with fieldErrors when the name is blank`() {
            val request = aCreateEventRequest(name = "   ")

            mockMvc.post("/events") {
                contentType = MediaType.APPLICATION_JSON
                content = jsonMapper.writeValueAsString(request)
            }
                .andExpect {
                    status { isBadRequest() }
                    jsonPath("$.message") { value("One or more fields are invalid") }
                    jsonPath("$.fieldErrors.length()") { value(1) }
                    jsonPath("$.fieldErrors[0].field") { value("name") }
                    jsonPath("$.fieldErrors[0].message") { value("must not be blank") }
                }

            verifyNoInteractions(createEventUseCase)
        }

        @Test
        fun `should return 400 when startsAt is in the past`() {
            val request = aCreateEventRequest(startsAt = Instant.parse("2025-03-20T23:00:00Z"))

            mockMvc.post("/events") {
                contentType = MediaType.APPLICATION_JSON
                content = jsonMapper.writeValueAsString(request)
            }
                .andExpect {
                    status { isBadRequest() }
                    jsonPath("$.message") { value("One or more fields are invalid") }
                    jsonPath("$.fieldErrors.length()") { value(1) }
                    jsonPath("$.fieldErrors[0].field") { value("startsAt") }
                    jsonPath("$.fieldErrors[0].message") { value("must be a future date") }
                    jsonPath("$.fieldErrors[0].rejectedValue") { value("2025-03-20T23:00:00Z") }
                }

            verifyNoInteractions(createEventUseCase)
        }

        @Test
        fun `should return 400 naming the field when organizerId is missing`() {

            val body = jsonMapper.writeValueAsString(
                mapOf(
                    "name" to "Festival de Verão",
                    "venue" to "Arena Central",
                    "city" to "Salvador",
                    "startsAt" to REQUEST_EVENT_STARTS_AT.toString(),
                )
            )

            mockMvc.post("/events") {
                contentType = MediaType.APPLICATION_JSON
                content = body
            }
                .andExpect {
                    status { isBadRequest() }
                    jsonPath("$.message") { value("One or more fields are invalid") }
                    jsonPath("$.fieldErrors.length()") { value(1) }
                    jsonPath("$.fieldErrors[0].field") { value("organizerId") }
                    jsonPath("$.fieldErrors[0].message") { value("is missing or has an invalid format") }
                    jsonPath("$.fieldErrors[0].rejectedValue") { doesNotExist() }
                }

            verifyNoInteractions(createEventUseCase)
        }

        @Test
        fun `should return 400 when the JSON is malformed`() {
            val body = """{"name": "Festival de Verão", """

            mockMvc.post("/events") {
                contentType = MediaType.APPLICATION_JSON
                content = body
            }
                .andExpect {
                    status { isBadRequest() }
                    jsonPath("$.status") { value(400) }
                    jsonPath("$.message") { value("Malformed JSON") }
                    jsonPath("$.path") { value("/events") }
                    jsonPath("$.fieldErrors") { doesNotExist() }
                }

            verifyNoInteractions(createEventUseCase)
        }
    }

    @Nested
    inner class AddSector {

        @Test
        fun `should return 201 with the created sector`() {

            val request = anAddSectorRequest()
            val event = anEvent()
            val expectedCommand = anAddSectorCommand(event.id)
            val expectedSector = aSector()

            given(addSectorUseCase.addSector(expectedCommand)).willReturn(expectedSector)

            mockMvc.post("/events/{eventId}/sectors", event.id) {
                contentType = MediaType.APPLICATION_JSON
                content = jsonMapper.writeValueAsString(request)
            }
                .andExpect {
                    status { isCreated() }
                    header { string(HttpHeaders.LOCATION, "${BASE_URL}/${event.id}/sectors/${expectedSector.id}") }
                    jsonPath("$.id") { value(expectedSector.id.toString()) }
                    jsonPath("$.name") { value(expectedSector.name) }
                    jsonPath("$.lots") { isEmpty() }
                }

            then(addSectorUseCase).should().addSector(expectedCommand)
        }

        @Test
        fun `should return 404 when the event does not exist`() {
            val request = anAddSectorRequest()
            val eventId = UUID.randomUUID()

            given(addSectorUseCase.addSector(anAddSectorCommand(eventId, name = request.name)))
                .willThrow(EventNotFoundException(eventId))

            mockMvc.post("/events/{eventId}/sectors", eventId) {
                contentType = MediaType.APPLICATION_JSON
                content = jsonMapper.writeValueAsString(request)
            }
                .andExpect {
                    status { isNotFound() }
                    jsonPath("$.status") { value(404) }
                    jsonPath("$.error") { value("Not Found") }
                    jsonPath("$.message") { value("Event $eventId not found") }
                    jsonPath("$.path") { value("/events/$eventId/sectors") }
                }
        }

        @Test
        fun `should return 422 when the sector name is already in use`() {
            val request = anAddSectorRequest(name = "Pista")
            val eventId = UUID.randomUUID()

            given(addSectorUseCase.addSector(anAddSectorCommand(eventId, name = request.name)))
                .willThrow(DuplicateSectorNameException(request.name))

            mockMvc.post("/events/{eventId}/sectors", eventId) {
                contentType = MediaType.APPLICATION_JSON
                content = jsonMapper.writeValueAsString(request)
            }
                .andExpect {
                    status { isUnprocessableContent() }
                    jsonPath("$.status") { value(422) }
                    jsonPath("$.message") { value("A sector named 'Pista' already exists in the event") }
                }
        }

        @Test
        fun `should return 400 when eventId in the URL is not a UUID`() {
            mockMvc.post("/events/{eventId}/sectors", "abc") {
                contentType = MediaType.APPLICATION_JSON
                content = jsonMapper.writeValueAsString(anAddSectorRequest())
            }
                .andExpect {
                    status { isBadRequest() }
                    jsonPath("$.message") { value("One or more fields are invalid") }
                    jsonPath("$.fieldErrors.length()") { value(1) }
                    jsonPath("$.fieldErrors[0].field") { value("eventId") }
                    jsonPath("$.fieldErrors[0].message") { value("must be a valid UUID") }
                    jsonPath("$.fieldErrors[0].rejectedValue") { value("abc") }
                }

            verifyNoInteractions(addSectorUseCase)
        }
        }

        @Nested
        inner class AddLot {

            @Test
            fun `should return 201 with the created lot`() {
                val request = anAddLotRequest()
                val sector = aSector()
                val event = anEvent().apply { this.addSector(sector) }

                val expectedCommand = anAddLotCommand(
                    eventId = event.id,
                    sectorId = sector.id,
                    startsAt = request.startsAt,
                    endsAt = request.endsAt
                )

                val expectedLot = aLot()

                given(addLotUseCase.addLot(expectedCommand)).willReturn(expectedLot)

                mockMvc.post("/events/{eventId}/sectors/{sectorId}/lots", event.id, sector.id) {
                    contentType = MediaType.APPLICATION_JSON
                    content = jsonMapper.writeValueAsString(request)
                }
                    .andExpect {
                        status { isCreated() }
                        header { string(HttpHeaders.LOCATION, "${BASE_URL}/${event.id}/sectors/${sector.id}/lots/${expectedLot.id}") }
                        jsonPath("$.id") { value(expectedLot.id.toString()) }
                        jsonPath("$.name") { value(expectedLot.name) }
                        jsonPath("$.priceCents") { value(expectedLot.priceCents) }
                        jsonPath("$.quantity") { value(expectedLot.quantity) }
                        jsonPath("$.sortOrder") { value(expectedLot.sortOrder) }
                        jsonPath("$.startsAt") { value(expectedLot.startsAt.toString()) }
                        jsonPath("$.endsAt") { value(expectedLot.endsAt.toString()) }
                    }

                then(addLotUseCase).should().addLot(expectedCommand)
            }

            @Test
            fun `should return 400 when quantity is not positive`() {
                val request = anAddLotRequest(quantity = 0)

                mockMvc.post("/events/{eventId}/sectors/{sectorId}/lots", UUID.randomUUID(), UUID.randomUUID()) {
                    contentType = MediaType.APPLICATION_JSON
                    content = jsonMapper.writeValueAsString(request)
                }
                    .andExpect {
                        status { isBadRequest() }
                        jsonPath("$.message") { value("One or more fields are invalid") }
                        jsonPath("$.fieldErrors.length()") { value(1) }
                        jsonPath("$.fieldErrors[0].field") { value("quantity") }
                        jsonPath("$.fieldErrors[0].message") { value("must be greater than 0") }
                        jsonPath("$.fieldErrors[0].rejectedValue") { value(0) }
                    }

                verifyNoInteractions(addLotUseCase)
            }

            @Test
            fun `should return 400 when endsAt is not after startsAt`() {
                val request = anAddLotRequest(startsAt = REQUEST_LOT_ENDS_AT, endsAt = REQUEST_LOT_STARTS_AT)
                val eventId = UUID.randomUUID()
                val sectorId = UUID.randomUUID()

                given(addLotUseCase.addLot(commandFor(eventId, sectorId, request)))
                    .willThrow(IllegalArgumentException("EndsAt must be after startsAt"))

                mockMvc.post("/events/{eventId}/sectors/{sectorId}/lots", eventId, sectorId) {
                    contentType = MediaType.APPLICATION_JSON
                    content = jsonMapper.writeValueAsString(request)
                }
                    .andExpect {
                        status { isBadRequest() }
                        jsonPath("$.message") { value("EndsAt must be after startsAt") }
                        jsonPath("$.fieldErrors") { doesNotExist() }
                    }
            }

            @Test
            fun `should return 404 when the sector does not belong to the event`() {
                val request = anAddLotRequest()
                val eventId = UUID.randomUUID()
                val sectorId = UUID.randomUUID()

                given(addLotUseCase.addLot(commandFor(eventId, sectorId, request)))
                    .willThrow(SectorNotFoundException(sectorId))

                mockMvc.post("/events/{eventId}/sectors/{sectorId}/lots", eventId, sectorId) {
                    contentType = MediaType.APPLICATION_JSON
                    content = jsonMapper.writeValueAsString(request)
                }
                    .andExpect {
                        status { isNotFound() }
                        jsonPath("$.status") { value(404) }
                        jsonPath("$.error") { value("Not Found") }
                        jsonPath("$.message") { value("Sector $sectorId not found in the event") }
                        jsonPath("$.path") { value("/events/$eventId/sectors/$sectorId/lots") }
                    }
            }

            @Test
            fun `should return 422 when the sale window overlaps another lot`() {
                val request = anAddLotRequest(name = "2º lote")
                val eventId = UUID.randomUUID()
                val sectorId = UUID.randomUUID()

                given(addLotUseCase.addLot(commandFor(eventId, sectorId, request)))
                    .willThrow(OverlappingLotWindowException(aLot(name = "2º lote"), aLot(name = "1º lote")))

                mockMvc.post("/events/{eventId}/sectors/{sectorId}/lots", eventId, sectorId) {
                    contentType = MediaType.APPLICATION_JSON
                    content = jsonMapper.writeValueAsString(request)
                }
                    .andExpect {
                        status { isUnprocessableContent() }
                        jsonPath("$.message") { value("Lot '2º lote' overlaps the sale window of lot '1º lote'") }
                    }
            }

            private fun commandFor(eventId: UUID, sectorId: UUID, request: AddLotRequestDTO) = anAddLotCommand(
                eventId = eventId,
                sectorId = sectorId,
                name = request.name,
                priceCents = request.priceCents,
                quantity = request.quantity,
                sortOrder = request.sortOrder,
                startsAt = request.startsAt,
                endsAt = request.endsAt,
            )
        }

        @Nested
        inner class PublishEvent {

            @Test
            fun `should return 200 with the event PUBLISHED`() {
                // Built through the domain itself, so the returned event is a real PUBLISHED one.
                val publishedEvent = anEvent().apply {
                    addSector(aSector(lots = listOf(aLot())))
                    publish()
                }

                given(publishEventUseCase.publish(PublishEventCommand(publishedEvent.id))).willReturn(publishedEvent)

                mockMvc.post("/events/{eventId}/publish", publishedEvent.id)
                    .andExpect {
                        status { isOk() }
                        header { doesNotExist(HttpHeaders.LOCATION) }
                        jsonPath("$.id") { value(publishedEvent.id.toString()) }
                        jsonPath("$.status") { value("PUBLISHED") }
                        jsonPath("$.sectors.length()") { value(1) }
                        jsonPath("$.sectors[0].name") { value("Pista") }
                        jsonPath("$.sectors[0].lots.length()") { value(1) }
                    }

                then(publishEventUseCase).should().publish(PublishEventCommand(publishedEvent.id))
            }

            @Test
            fun `should return 404 when the event does not exist`() {
                val eventId = UUID.randomUUID()

                given(publishEventUseCase.publish(PublishEventCommand(eventId)))
                    .willThrow(EventNotFoundException(eventId))

                mockMvc.post("/events/{eventId}/publish", eventId)
                    .andExpect {
                        status { isNotFound() }
                        jsonPath("$.status") { value(404) }
                        jsonPath("$.error") { value("Not Found") }
                        jsonPath("$.message") { value("Event $eventId not found") }
                        jsonPath("$.path") { value("/events/$eventId/publish") }
                    }
            }

            @Test
            fun `should return 422 when the event has no sectors`() {
                val eventId = UUID.randomUUID()

                given(publishEventUseCase.publish(PublishEventCommand(eventId)))
                    .willThrow(EventHasNoSectorsException(eventId))

                mockMvc.post("/events/{eventId}/publish", eventId)
                    .andExpect {
                        status { isUnprocessableContent() }
                        jsonPath("$.status") { value(422) }
                        jsonPath("$.message") { value("Event $eventId cannot be published without sectors") }
                    }
            }

            @Test
            fun `should return 405 when the method is GET`() {
                mockMvc.get("/events/{eventId}/publish", UUID.randomUUID())
                    .andExpect {
                        status { isMethodNotAllowed() }
                        jsonPath("$.status") { value(405) }
                        jsonPath("$.error") { value("Method Not Allowed") }
                        jsonPath("$.message") { value("Method 'GET' is not supported.") }
                    }

                verifyNoInteractions(publishEventUseCase)
            }
        }
    }