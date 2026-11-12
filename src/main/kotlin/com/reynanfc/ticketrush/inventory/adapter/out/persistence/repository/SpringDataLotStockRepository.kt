package com.reynanfc.ticketrush.inventory.adapter.out.persistence.repository

import com.reynanfc.ticketrush.inventory.adapter.out.persistence.entity.LotStockEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

// Spring Data's repository, used only inside this adapter. The core never sees it.
interface SpringDataLotStockRepository : JpaRepository<LotStockEntity, UUID>
