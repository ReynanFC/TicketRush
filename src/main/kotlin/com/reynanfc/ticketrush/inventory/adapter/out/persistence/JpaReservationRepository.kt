package com.reynanfc.ticketrush.inventory.adapter.out.persistence

import com.reynanfc.ticketrush.inventory.adapter.out.persistence.mapper.InventoryPersistenceMapper
import com.reynanfc.ticketrush.inventory.adapter.out.persistence.repository.SpringDataReservationRepository
import com.reynanfc.ticketrush.inventory.application.port.out.ReservationRepository
import com.reynanfc.ticketrush.inventory.domain.model.Reservation
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// Output adapter: implements the core's ReservationRepository port with JPA.
@Repository
class JpaReservationRepository(
    private val springDataRepository: SpringDataReservationRepository,
) : ReservationRepository {

    @Transactional
    override fun save(reservation: Reservation) {
        springDataRepository.save(InventoryPersistenceMapper.toEntity(reservation))
    }

    @Transactional(readOnly = true)
    override fun findById(id: UUID): Reservation? {
        return springDataRepository.findByIdOrNull(id)
            ?.let(InventoryPersistenceMapper::toDomain)
    }
}
