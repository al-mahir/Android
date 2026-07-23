package com.example.mushaf.data.prefs

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mushaf.data.MushafLog
import com.example.mushaf.domain.model.recite.MoshafValue
import com.example.mushaf.domain.model.recite.RecitationSettings
import com.example.mushaf.domain.model.recite.RecitationStrictness
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

private val Context.recitationSettingsStore: DataStore<Preferences> by preferencesDataStore(
    name = "recitation_settings",
)


class RecitationSettingsDataStore(context: Context) {

    private val dataStore = context.applicationContext.recitationSettingsStore

    val settings: Flow<RecitationSettings> = dataStore.data.map { prefs ->
        RecitationSettings(
            engine = prefs[KEY_ENGINE],
            strictness = prefs[KEY_STRICTNESS].toStrictness(),
            tajweedGradingEnabled = prefs[KEY_TAJWEED_GRADING] ?: true,
            gradedRules = prefs[KEY_RULES]?.decodeRules(),
            moshaf = prefs[KEY_MOSHAF]?.decodeMoshaf().orEmpty(),
        )
    }

    suspend fun update(settings: RecitationSettings) {
        dataStore.edit { prefs ->
            settings.engine
                ?.let { prefs[KEY_ENGINE] = it }
                ?: prefs.remove(KEY_ENGINE)

            prefs[KEY_STRICTNESS] = settings.strictness.wireValue
            prefs[KEY_TAJWEED_GRADING] = settings.tajweedGradingEnabled

            
            settings.gradedRules
                ?.let { prefs[KEY_RULES] = JsonArray(it.map(::JsonPrimitive)).toString() }
                ?: prefs.remove(KEY_RULES)

            prefs[KEY_MOSHAF] = JsonObject(
                settings.moshaf.mapValues { (_, value) ->
                    when (value) {
                        is MoshafValue.Text -> JsonPrimitive(value.value)
                        is MoshafValue.Number -> JsonPrimitive(value.value)
                    }
                },
            ).toString()
        }
    }

    private fun String?.toStrictness(): RecitationStrictness =
        RecitationStrictness.entries.firstOrNull { it.wireValue == this }
            ?: RecitationStrictness.NORMAL


    private fun String.decodeRules(): Set<String>? = runCatching {
        (json.parseToJsonElement(this) as JsonArray).map { (it as JsonPrimitive).content }.toSet()
    }.getOrElse {
        Log.w(TAG, "Discarding corrupt stored rules selection; grading everything", it)
        null
    }

    private fun String.decodeMoshaf(): Map<String, MoshafValue> = runCatching {
        (json.parseToJsonElement(this) as JsonObject).mapValues { (_, element) ->
            val primitive = element as JsonPrimitive
            primitive.intOrNull?.let(MoshafValue::Number) ?: MoshafValue.Text(primitive.content)
        }
    }.getOrElse {
        Log.w(TAG, "Discarding corrupt stored moshaf config; using server defaults", it)
        emptyMap()
    }

    private companion object {
        const val TAG = MushafLog.TAG
        val json = Json

        val KEY_ENGINE = stringPreferencesKey("engine")
        val KEY_STRICTNESS = stringPreferencesKey("strictness")
        val KEY_TAJWEED_GRADING = booleanPreferencesKey("tajweed_grading_enabled")
        val KEY_RULES = stringPreferencesKey("rules_json")
        val KEY_MOSHAF = stringPreferencesKey("moshaf_json")
    }
}
