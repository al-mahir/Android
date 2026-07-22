package com.example.mushaf.data.repository

import com.example.mushaf.data.recitation.RecitationDataSource
import com.example.mushaf.data.recitation.RecitationMapper
import com.example.mushaf.domain.model.AyahTiming
import com.example.mushaf.domain.model.Reciter
import com.example.mushaf.domain.repository.RecitationRepository
import com.iti.domain.core.Result
import com.iti.domain.core.DomainError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class RecitationRepositoryImpl(
    private val dataSource: RecitationDataSource
) : RecitationRepository {

    override fun getReciters(): Flow<Result<List<Reciter>>> {
        return dataSource.observeReciters()
            .map { dtoList ->
                val reciters = dtoList.map { RecitationMapper.toDomain(it) }
                Result.Success(reciters) as Result<List<Reciter>>
            }
            .catch { e ->
                emit(Result.Error(DomainError.NetworkError(e)))
            }
    }

    override fun getTimingsForPage(reciterId: Int, pageNumber: Int): Flow<Result<List<AyahTiming>>> {
        return dataSource.observeTimingsForPage(reciterId, pageNumber)
            .map { dtoList ->
                val timings = dtoList.map { RecitationMapper.toDomain(it) }
                Result.Success(timings) as Result<List<AyahTiming>>
            }
            .catch { e ->
                emit(Result.Error(DomainError.NetworkError(e)))
            }
    }
}
