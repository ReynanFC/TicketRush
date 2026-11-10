package com.reynanfc.ticketrush.catalog.adapter.out.persistence.repository

import com.reynanfc.ticketrush.catalog.adapter.out.persistence.entity.EventEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

// Spring Data's repository, used only inside this adapter. The core never sees it.
interface SpringDataEventRepository : JpaRepository<EventEntity, UUID>
