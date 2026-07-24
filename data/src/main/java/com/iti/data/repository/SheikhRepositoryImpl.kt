package com.iti.data.repository

import com.iti.data.datasource.sheikh.SheikhDataSource
import com.iti.data.mapper.toDomain
import com.iti.domain.model.Sheikh
import com.iti.domain.repository.SheikhRepository

class SheikhRepositoryImpl(
    private val dataSource: SheikhDataSource,
) : SheikhRepository {

    override suspend fun getSheikhs(): List<Sheikh> =
        dataSource.getSheikhs().map { it.toDomain() }

    override suspend fun getSheikhById(id: String): Sheikh? =
        dataSource.getSheikhById(id)?.toDomain()

    override suspend fun searchSheikhs(name: String): List<Sheikh> =
        dataSource.searchSheikhs(name).map { it.toDomain() }
}
