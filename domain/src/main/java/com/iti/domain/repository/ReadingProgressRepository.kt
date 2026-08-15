package com.iti.domain.repository

import kotlinx.coroutines.flow.Flow


interface ReadingProgressRepository {
    fun observeLastPage(): Flow<Int?>
}
