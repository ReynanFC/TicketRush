package com.reynanfc.ticketrush.catalog.application.service

import com.reynanfc.ticketrush.catalog.domain.enums.EventStatus
import com.reynanfc.ticketrush.catalog.fixtures.CreateEventCommandFixtures.aCreateEventCommand
import com.reynanfc.ticketrush.catalog.fixtures.InMemoryEventRepository
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class CreateEventServiceTest {

    // A new instance per test (JUnit lifecycle), so each test starts with an empty repository.
    private val repository = InMemoryEventRepository()
    private val service = CreateEventService(repository)

    @Test
    fun `should save the created event`() {
        val event = service.create(aCreateEventCommand())

        assertNotNull(repository.findById(event.id))
    }

    @Test
    fun `should create the event as draft`() {
        val event = service.create(aCreateEventCommand())

        assertEquals(EventStatus.DRAFT, event.status)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   ", "\t", "\n"])
    fun `should not save an event with blank or empty name`(invalidName: String) {
        val invalidCommand = aCreateEventCommand(name = invalidName)

        assertFailsWith<IllegalArgumentException> {
            service.create(invalidCommand)
        }
        assertEquals(0, repository.count())
    }
}
