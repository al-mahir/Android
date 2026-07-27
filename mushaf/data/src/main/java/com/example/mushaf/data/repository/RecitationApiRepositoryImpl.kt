package com.example.mushaf.data.repository

import android.util.Log
import com.example.mushaf.data.MushafLog
import com.example.mushaf.data.recitation.RecitationDataSource
import com.example.mushaf.data.recitation.RecitationMapper
import com.example.mushaf.data.recite.RecitationSchemaMapper
import com.example.mushaf.data.recite.remote.AiServiceApi
import com.example.mushaf.domain.model.AyahTiming
import com.example.mushaf.domain.model.Reciter
import com.example.mushaf.domain.model.recite.RecitationSchema
import com.example.mushaf.domain.repository.RecitationRepository
import com.example.mushaf.domain.repository.RecitationSchemaRepository
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RecitationApiRepositoryImpl(
    private val dataSource: RecitationDataSource,
    private val api: AiServiceApi,
) : RecitationRepository, RecitationSchemaRepository {

    private val schemaMutex = Mutex()
    private var cachedSchema: RecitationSchema? = null

    // ── RecitationRepository ─────────────────────────────────────────────

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

    // ── RecitationSchemaRepository ────────────────────────────────────────

    override suspend fun schema(): Result<RecitationSchema> {
        cachedSchema?.let { return Result.Success(it) }

        return schemaMutex.withLock {
            cachedSchema?.let { return@withLock Result.Success(it) }
            try {
                val schema = fetchSchema()
                cachedSchema = schema
                Log.i(
                    TAG,
                    "Recitation schema: engines=${schema.engines.map { it.key }} " +
                        "rules=${schema.rules.size} moshafFields=${schema.moshafFields.size}",
                )
                Result.Success(schema)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Log.w(TAG, "Could not load the recitation schema", throwable)
                Result.Error(DomainError.NetworkError(throwable))
            }
        }
    }

    private suspend fun fetchSchema(): RecitationSchema = coroutineScope {
        val health = async { api.health() }
        val rules = async { api.tajweedRules() }
        val moshaf = async { api.moshafSchema() }
        RecitationSchemaMapper.toSchema(
            health = health.await(),
            rules = rules.await(),
            moshaf = moshaf.await(),
        )
    }

    private companion object {
        const val TAG = MushafLog.TAG
    }
}
