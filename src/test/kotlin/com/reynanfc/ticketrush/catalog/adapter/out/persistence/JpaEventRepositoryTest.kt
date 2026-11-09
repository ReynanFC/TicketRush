package com.reynanfc.ticketrush.catalog.adapter.out.persistence

import com.reynanfc.ticketrush.TestcontainersConfiguration
import com.reynanfc.ticketrush.catalog.domain.enums.EventStatus
import com.reynanfc.ticketrush.catalog.fixtures.EventFixtures.anEvent
import com.reynanfc.ticketrush.catalog.fixtures.LotFixtures.LOT_ENDS_AT
import com.reynanfc.ticketrush.catalog.fixtures.LotFixtures.aLot
import com.reynanfc.ticketrush.catalog.fixtures.SectorFixtures.aSector
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

// Integration test against a real Postgres (Testcontainers) with the V1 schema applied by Flyway.
//
// @DataJpaTest wraps every test in a transaction by default. NOT_SUPPORTED turns that off, so each
// call to the adapter runs on its own, like in production: lazy loading outside a transaction fails
// here too, and reads really hit the database instead of Hibernate's first-level cache.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration::class, JpaEventRepository::class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JpaEventRepositoryTest {

    @Autowired
    private lateinit var repository: JpaEventRepository

    @Autowired
    private lateinit var springDataRepository: SpringDataEventRepository

    // No rollback without a test transaction, so clean up by hand. Cascade removes sectors and lots.
    @AfterEach
    fun cleanUp() {
        springDataRepository.deleteAll()
    }

    @Test
    fun `should save and find a draft event with its sectors and lots`() {
        val lot = aLot(name = "Promocional", priceCents = 5_000, quantity = 50)
        val sector = aSector(name = "Pista", lots = listOf(lot))
        val event = anEvent().apply { addSector(sector) }

        repository.save(event)
        val found = assertNotNull(repository.findById(event.id))

        assertEquals(event.id, found.id)
        assertEquals(event.organizerId, found.organizerId)
        assertEquals(event.name, found.name)
        assertEquals(event.venue, found.venue)
        assertEquals(event.city, found.city)
        assertEquals(event.startsAt, found.startsAt)
        assertEquals(EventStatus.DRAFT, found.status)

        val foundSector = found.sectors.single()
        assertEquals(sector.id, foundSector.id)
        assertEquals("Pista", foundSector.name)

        val foundLot = foundSector.lots.single()
        assertEquals(lot.id, foundLot.id)
        assertEquals("Promocional", foundLot.name)
        assertEquals(5_000, foundLot.priceCents)
        assertEquals(50, foundLot.quantity)
        assertEquals(lot.sortOrder, foundLot.sortOrder)
        assertEquals(lot.startsAt, foundLot.startsAt)
        assertEquals(lot.endsAt, foundLot.endsAt)
    }

    @Test
    fun `should return null when the event does not exist`() {
        assertNull(repository.findById(UUID.randomUUID()))
    }

    @Test
    fun `should restore a published event`() {
        val event = anEvent().apply {
            addSector(aSector(name = "Pista", lots = listOf(aLot())))
            publish()
        }

        repository.save(event)
        val found = assertNotNull(repository.findById(event.id))

        assertEquals(EventStatus.PUBLISHED, found.status)
    }

    @Test
    fun `should update an event that already exists`() {
        val event = anEvent().apply { addSector(aSector(name = "Pista", lots = listOf(aLot()))) }
        repository.save(event)

        // The same flow as the use cases: load, change through the domain, save again.
        val loaded = assertNotNull(repository.findById(event.id))
        val pistaId = loaded.sectors.single().id
        loaded.addSector(aSector(name = "Camarote", lots = listOf(aLot())))
        loaded.addLot(pistaId, aLot(sortOrder = 2, startsAt = LOT_ENDS_AT, endsAt = LOT_ENDS_AT.plusSeconds(3600)))
        loaded.publish()
        repository.save(loaded)

        val found = assertNotNull(repository.findById(event.id))
        assertEquals(EventStatus.PUBLISHED, found.status)
        assertEquals(setOf("Pista", "Camarote"), found.sectors.map { it.name }.toSet())
        assertEquals(2, found.sectors.single { it.id == pistaId }.lots.size)
        assertEquals(1, springDataRepository.count())
    }
}
