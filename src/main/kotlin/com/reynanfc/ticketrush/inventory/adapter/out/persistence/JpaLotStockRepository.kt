package com.reynanfc.ticketrush.inventory.adapter.out.persistence

import com.reynanfc.ticketrush.inventory.adapter.out.persistence.mapper.InventoryPersistenceMapper
import com.reynanfc.ticketrush.inventory.adapter.out.persistence.repository.SpringDataLotStockRepository
import com.reynanfc.ticketrush.inventory.application.port.out.LotStockRepository
import com.reynanfc.ticketrush.inventory.domain.model.LotStock
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// Output adapter: implements the core's LotStockRepository port with JPA.
// @Transactional joins the use case's transaction when there is one (propagation REQUIRED).
@Repository
class JpaLotStockRepository(
    private val springDataRepository: SpringDataLotStockRepository,
) : LotStockRepository {

    @Transactional
    override fun save(stock: LotStock) {
        springDataRepository.save(InventoryPersistenceMapper.toEntity(stock))
    }

    @Transactional(readOnly = true)
    override fun findByLotId(lotId: UUID): LotStock? {
        return springDataRepository.findByIdOrNull(lotId)
            ?.let(InventoryPersistenceMapper::toDomain)
    }
}
