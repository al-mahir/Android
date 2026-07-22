package com.example.mushaf.data.repository

import android.util.Log
import com.example.mushaf.data.MushafLog
import com.example.mushaf.data.recite.RecitationSchemaMapper
import com.example.mushaf.data.recite.remote.AiServiceApi
import com.example.mushaf.domain.model.recite.RecitationSchema
import com.example.mushaf.domain.repository.RecitationSchemaRepository
import com.iti.domain.core.DomainError
import com.iti.domain.core.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class RecitationSchemaRepositoryImpl(
    private val api: AiServiceApi,
) : RecitationSchemaRepository {

    private val mutex = Mutex()
    private var cached: RecitationSchema? = null

    override suspend fun schema(): Result<RecitationSchema> {
        cached?.let { return Result.Success(it) }

        return mutex.withLock {
            cached?.let { return@withLock Result.Success(it) }
            try {
                val schema = fetch()
                cached = schema
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

    private suspend fun fetch(): RecitationSchema = coroutineScope {
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
