package com.reynanfc.ticketrush.catalog.adapter.out.persistence

import com.reynanfc.ticketrush.catalog.adapter.out.persistence.mapper.EventPersistenceMapper
import com.reynanfc.ticketrush.catalog.adapter.out.persistence.repository.SpringDataEventRepository
import com.reynanfc.ticketrush.catalog.application.port.out.EventRepository
import com.reynanfc.ticketrush.catalog.domain.model.Event
import java.util.UUID
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

// Output adapter: implements the core's EventRepository port with JPA.
@Repository
class JpaEventRepository(
    private val springDataRepository: SpringDataEventRepository,
) : EventRepository {

    @Transactional
    override fun save(event: Event) {
        springDataRepository.save(EventPersistenceMapper.toEntity(event))
    }

    @Transactional(readOnly = true)
    override fun findById(id: UUID): Event? {
        return springDataRepository.findByIdOrNull(id)
            ?.let(EventPersistenceMapper::toDomain)
    }
}
