package com.reynanfc.ticketrush.catalog.fixtures.web

import com.reynanfc.ticketrush.catalog.adapter.`in`.web.dto.request.AddSectorRequestDTO

object AddSectorRequestDTOFixtures {

    fun anAddSectorRequest(
        name: String = "Pista",
    ) = AddSectorRequestDTO(name = name)
}
