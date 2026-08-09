package com.iti.data.exam

import com.iti.data.local.exam.ExamSummaryDao
import com.iti.data.local.exam.ExamSummaryEntity
import com.iti.domain.model.exam.ExamMistake
import com.iti.domain.model.exam.ExamMistakeCategory
import com.iti.domain.model.exam.ExamQuestion
import com.iti.domain.model.exam.ExamQuestionResult
import com.iti.domain.model.exam.ExamScope
import com.iti.domain.model.exam.ExamSummary
import com.iti.domain.model.exam.RecentExamScope
import com.iti.domain.model.exam.WordFeedback
import com.iti.domain.model.exam.WordStatus
import com.iti.domain.model.quran.SurahNames
import com.iti.domain.repository.ExamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

class ExamRepositoryImpl(
    private val dao: ExamSummaryDao,
    private val prefs: android.content.SharedPreferences,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : ExamRepository {

    override suspend fun save(summary: ExamSummary) {
        dao.insert(summary.toEntity())
    }

    override fun observeRecentScopes(): Flow<List<RecentExamScope>> =
        dao.observeRecent(5).map { entities ->
            entities.map { entity ->
                val scope = entity.scopeJson.toScope()
                val label = buildScopeLabel(scope)
                RecentExamScope(
                    id = entity.id,
                    scope = scope,
                    displayLabel = label,
                    timestampMs = entity.startedAtMs,
                )
            }
        }

    override suspend fun getById(id: String): ExamSummary? {
        val entity = dao.getById(id) ?: return null
        return ExamSummary(
            id = entity.id,
            scope = entity.scopeJson.toScope(),
            startedAtMs = entity.startedAtMs,
            totalDurationMs = entity.totalDurationMs,
            questionResults = entity.questionResultsJson.toQuestionResults()
        )
    }

    override suspend fun saveCustomScope(scope: ExamScope.CustomRange) {
        val list = getCustomScopes().toMutableList()
        list.removeAll { it.id == scope.id }
        list.add(scope)
        val jsonStr = buildJsonArray {
            list.forEach { add(json.parseToJsonElement(it.toJson())) }
        }.toString()
        prefs.edit().putString("custom_scopes", jsonStr).apply()
    }

    override suspend fun getCustomScopes(): List<ExamScope.CustomRange> {
        val jsonStr = prefs.getString("custom_scopes", "[]") ?: "[]"
        return try {
            val arr = json.parseToJsonElement(jsonStr).jsonArray
            arr.mapNotNull { it.toString().toScope() as? ExamScope.CustomRange }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun deleteCustomScope(id: String) {
        val list = getCustomScopes().toMutableList()
        list.removeAll { it.id == id }
        val jsonStr = buildJsonArray {
            list.forEach { add(json.parseToJsonElement(it.toJson())) }
        }.toString()
        prefs.edit().putString("custom_scopes", jsonStr).apply()
    }


    private fun ExamSummary.toEntity(): ExamSummaryEntity = ExamSummaryEntity(
        id = id,
        scopeJson = scope.toJson(),
        startedAtMs = startedAtMs,
        totalDurationMs = totalDurationMs,
        totalQuestions = totalQuestions,
        correctCount = correctCount,
        mistakeCount = mistakeCount,
        skippedCount = skippedCount,
        accuracy = accuracy,
        questionResultsJson = questionResults.toJson(),
    )


    private fun ExamScope.toJson(): String = buildJsonObject {
        when (this@toJson) {
            is ExamScope.SingleSurah -> { put("type", "SingleSurah"); put("surah", surahNumber) }
            is ExamScope.SurahRange -> { put("type", "SurahRange"); put("from", fromSurah); put("to", toSurah) }
            is ExamScope.MultiSurah -> { put("type", "MultiSurah"); put("surahs", buildJsonArray { surahNumbers.forEach { add(JsonPrimitive(it)) } }) }
            is ExamScope.SingleJuz -> { put("type", "SingleJuz"); put("juz", juzNumber) }
            is ExamScope.JuzRange -> { put("type", "JuzRange"); put("from", fromJuz); put("to", toJuz) }
            is ExamScope.SingleRub -> { put("type", "SingleRub"); put("rub", rubNumber) }
            is ExamScope.AyahRange -> { put("type", "AyahRange"); put("ss", startSurah); put("sa", startAyah); put("es", endSurah); put("ea", endAyah) }
            is ExamScope.CustomRange -> { 
                put("type", "CustomRange")
                put("id", id)
                put("name", name)
                put("components", buildJsonArray { components.forEach { add(json.parseToJsonElement(it.toJson())) } })
            }
        }
    }.toString()

    private fun String.toScope(): ExamScope {
        val obj = json.parseToJsonElement(this).jsonObject
        return when (obj["type"]?.jsonPrimitive?.content) {
            "SingleSurah" -> ExamScope.SingleSurah(obj["surah"]!!.jsonPrimitive.int)
            "SurahRange" -> ExamScope.SurahRange(obj["from"]!!.jsonPrimitive.int, obj["to"]!!.jsonPrimitive.int)
            "MultiSurah" -> ExamScope.MultiSurah(obj["surahs"]!!.jsonArray.map { it.jsonPrimitive.int })
            "SingleJuz" -> ExamScope.SingleJuz(obj["juz"]!!.jsonPrimitive.int)
            "JuzRange" -> ExamScope.JuzRange(obj["from"]!!.jsonPrimitive.int, obj["to"]!!.jsonPrimitive.int)
            "SingleRub" -> ExamScope.SingleRub(obj["rub"]!!.jsonPrimitive.int)
            "AyahRange" -> ExamScope.AyahRange(obj["ss"]!!.jsonPrimitive.int, obj["sa"]!!.jsonPrimitive.int, obj["es"]!!.jsonPrimitive.int, obj["ea"]!!.jsonPrimitive.int)
            "CustomRange" -> ExamScope.CustomRange(
                id = obj["id"]!!.jsonPrimitive.content,
                name = obj["name"]!!.jsonPrimitive.content,
                components = obj["components"]!!.jsonArray.map { it.toString().toScope() }
            )
            else -> ExamScope.SingleSurah(1)
        }
    }

    private fun buildScopeLabel(scope: ExamScope): String = when (scope) {
        is ExamScope.SingleSurah -> SurahNames.nameOf(scope.surahNumber) ?: "سورة ${scope.surahNumber}"
        is ExamScope.SurahRange -> "سورة ${SurahNames.nameOf(scope.fromSurah)} - ${SurahNames.nameOf(scope.toSurah)}"
        is ExamScope.MultiSurah -> "سور متعددة"
        is ExamScope.SingleJuz -> "جزء ${scope.juzNumber}"
        is ExamScope.JuzRange -> "أجزاء ${scope.fromJuz} - ${scope.toJuz}"
        is ExamScope.SingleRub -> "حزب ${scope.rubNumber}"
        is ExamScope.AyahRange -> {
            val start = "${SurahNames.nameOf(scope.startSurah)} ${scope.startAyah}"
            val end = "${SurahNames.nameOf(scope.endSurah)} ${scope.endAyah}"
            "$start - $end"
        }
        is ExamScope.CustomRange -> scope.name
    }


    private fun List<ExamQuestionResult>.toJson(): String = buildJsonArray {
        forEach { result ->
            add(buildJsonObject {
                put("surah", result.question.surahNumber)
                put("ayah", result.question.ayahNumber)
                put("index", result.question.index)
                put("durationMs", result.durationMs)
                put("skipped", result.skipped)
                put("mistakeCount", result.mistakeCount)
            })
        }
    }.toString()
    
    private fun String.toQuestionResults(): List<ExamQuestionResult> {
        return try {
            val arr = json.parseToJsonElement(this).jsonArray
            arr.map { elem ->
                val obj = elem.jsonObject
                ExamQuestionResult(
                    question = ExamQuestion(
                        index = obj["index"]?.jsonPrimitive?.int ?: 0,
                        surahNumber = obj["surah"]?.jsonPrimitive?.int ?: 1,
                        ayahNumber = obj["ayah"]?.jsonPrimitive?.int ?: 1,
                    ),
                    durationMs = obj["durationMs"]?.jsonPrimitive?.long ?: 0L,
                    skipped = obj["skipped"]?.jsonPrimitive?.boolean ?: false
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
