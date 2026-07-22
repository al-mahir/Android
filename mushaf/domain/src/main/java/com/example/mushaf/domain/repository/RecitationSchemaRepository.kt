package com.example.mushaf.domain.repository

import com.example.mushaf.domain.model.recite.RecitationSchema
import com.iti.domain.core.Result


interface RecitationSchemaRepository {

    suspend fun schema(): Result<RecitationSchema>
}
