package com.iti.data.datasource.sheikh

import com.iti.data.dto.sheikh.SheikhApiDto

interface SheikhDataSource {

    suspend fun getSheikhs(): List<SheikhApiDto>

    suspend fun getSheikhById(id: String): SheikhApiDto?

    suspend fun searchSheikhs(name: String): List<SheikhApiDto>
}
