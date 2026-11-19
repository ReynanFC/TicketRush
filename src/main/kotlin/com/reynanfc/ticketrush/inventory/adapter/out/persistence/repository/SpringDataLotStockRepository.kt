package com.reynanfc.ticketrush.inventory.adapter.out.persistence.repository

import com.reynanfc.ticketrush.inventory.adapter.out.persistence.entity.LotStockEntity
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.hibernate.cfg.AvailableSettings
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import java.util.UUID

// Spring Data's repository, used only inside this adapter. The core never sees it.
interface SpringDataLotStockRepository : JpaRepository<LotStockEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = AvailableSettings.JAKARTA_LOCK_TIMEOUT, value = "3000"))
    @Query("SELECT e FROM LotStockEntity e WHERE e.lotId = :id")
    fun findByIdWithLock(id: UUID): LotStockEntity?
}