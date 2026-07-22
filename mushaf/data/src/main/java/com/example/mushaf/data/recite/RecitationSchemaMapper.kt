package com.example.mushaf.data.recite

import com.example.mushaf.data.recite.remote.dto.HealthDto
import com.example.mushaf.data.recite.remote.dto.MoshafFieldDto
import com.example.mushaf.data.recite.remote.dto.MoshafSchemaDto
import com.example.mushaf.data.recite.remote.dto.TajweedRuleDto
import com.example.mushaf.data.recite.remote.dto.TajweedRulesResponseDto
import com.example.mushaf.domain.model.recite.MoshafFieldSpec
import com.example.mushaf.domain.model.recite.MoshafOptionSpec
import com.example.mushaf.domain.model.recite.MoshafValue
import com.example.mushaf.domain.model.recite.RecitationEngineSpec
import com.example.mushaf.domain.model.recite.RecitationSchema
import com.example.mushaf.domain.model.recite.TajweedRuleKind
import com.example.mushaf.domain.model.recite.TajweedRuleSpec
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

object RecitationSchemaMapper {

    private const val KIND_SIFA = "sifa"
    private const val MOCK_ENGINE = "mock"

    fun toSchema(
        health: HealthDto,
        rules: TajweedRulesResponseDto,
        moshaf: MoshafSchemaDto,
    ): RecitationSchema = RecitationSchema(
        engines = health.toEngines(),
        rules = rules.rules.map { it.toSpec() },
        
        
        moshafFields = moshaf.fields.filter { it.options.isNotEmpty() }.map { it.toSpec() },
    )


    private fun HealthDto.toEngines(): List<RecitationEngineSpec> =
        availableEngines
            .ifEmpty { listOf(engine) }
            .filter { it != MOCK_ENGINE }
            .distinct()
            .map { RecitationEngineSpec(key = it, isDefault = it == engine) }

    private fun TajweedRuleDto.toSpec() = TajweedRuleSpec(
        key = key,
        label = nameAr,
        kind = if (kind?.lowercase() == KIND_SIFA) TajweedRuleKind.SIFA else TajweedRuleKind.TAJWEED,
    )

    private fun MoshafFieldDto.toSpec() = MoshafFieldSpec(
        key = key,
        label = nameAr?.takeIf { it.isNotBlank() } ?: key,
        description = description?.takeIf { it.isNotBlank() },
        default = default?.toMoshafValue(),
        options = options.map { MoshafOptionSpec(it.value.toMoshafValue(), it.label) },
    )


    private fun JsonPrimitive.toMoshafValue(): MoshafValue =
        intOrNull?.let(MoshafValue::Number) ?: MoshafValue.Text(content)
}
