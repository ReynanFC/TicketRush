package com.reynanfc.ticketrush.catalog.adapter.`in`.web

import com.reynanfc.ticketrush.catalog.adapter.`in`.web.docs.EventControllerDocs
import com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.request.AddLotRequestDTO
import com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.request.AddSectorRequestDTO
import com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.request.CreateEventRequestDTO
import com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.response.EventResponseDTO
import com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.response.LotResponseDTO
import com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.response.SectorResponseDTO
import com.reynanfc.ticketrush.catalog.adapter.`in`.web.mapper.EventWebMapper
import com.reynanfc.ticketrush.catalog.application.port.`in`.command.PublishEventCommand
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.AddLotUseCase
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.AddSectorUseCase
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.CreateEventUseCase
import com.reynanfc.ticketrush.catalog.application.port.`in`.usecase.PublishEventUseCase
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.net.URI
import java.util.UUID

// Input adapter: translates HTTP into use case calls. No business rules here, only HTTP concerns.
// The OpenAPI documentation lives in EventControllerDocs.
@RestController
@RequestMapping("/events")
class EventController(
    private val createEventUseCase: CreateEventUseCase,
    private val addSectorUseCase: AddSectorUseCase,
    private val addLotUseCase: AddLotUseCase,
    private val publishEventUseCase: PublishEventUseCase,
) : EventControllerDocs {

    @PostMapping
    override fun createEvent(@Valid @RequestBody request: CreateEventRequestDTO): ResponseEntity<EventResponseDTO> {
        val event = createEventUseCase.create(EventWebMapper.toCommand(request))
        return ResponseEntity.created(locationOf(event.id)).body(EventWebMapper.toResponse(event))
    }

    @PostMapping("/{eventId}/sectors")
    override fun addSector(
        @PathVariable eventId: UUID,
        @Valid @RequestBody request: AddSectorRequestDTO,
    ): ResponseEntity<SectorResponseDTO> {
        val sector = addSectorUseCase.addSector(EventWebMapper.toCommand(eventId, request))
        return ResponseEntity.created(locationOf(sector.id)).body(EventWebMapper.toResponse(sector))
    }

    @PostMapping("/{eventId}/sectors/{sectorId}/lots")
    override fun addLot(
        @PathVariable eventId: UUID,
        @PathVariable sectorId: UUID,
        @Valid @RequestBody request: AddLotRequestDTO,
    ): ResponseEntity<LotResponseDTO> {
        val lot = addLotUseCase.addLot(EventWebMapper.toCommand(eventId, sectorId, request))
        return ResponseEntity.created(locationOf(lot.id)).body(EventWebMapper.toResponse(lot))
    }

    @PostMapping("/{eventId}/publish")
    override fun publishEvent(@PathVariable eventId: UUID): ResponseEntity<EventResponseDTO> {
        val event = publishEventUseCase.publish(PublishEventCommand(eventId))
        return ResponseEntity.ok(EventWebMapper.toResponse(event))
    }

    // Builds "<current request URL>/{id}", e.g. POST /events -> /events/{id}.
    private fun locationOf(id: UUID): URI =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(id)
            .toUri()
}
