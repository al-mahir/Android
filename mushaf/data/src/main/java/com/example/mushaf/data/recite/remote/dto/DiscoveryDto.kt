package com.example.mushaf.data.recite.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive






 
@Serializable
data class HealthDto(
    val status: String,
     
    val engine: String,
    @SerialName("available_engines") val availableEngines: List<String> = emptyList(),
    val device: String? = null,
    val dtype: String? = null,
    @SerialName("muaalem_model") val muaalemModel: String? = null,
    @SerialName("segmenter_model") val segmenterModel: String? = null,
)

 
@Serializable
data class TajweedRulesResponseDto(
    val rules: List<TajweedRuleDto> = emptyList(),
)

@Serializable
data class TajweedRuleDto(
     
    val key: String,
    @SerialName("name_ar") val nameAr: String,
    @SerialName("name_en") val nameEn: String? = null,
     
    val kind: String? = null,
)





 
@Serializable
data class MoshafSchemaDto(
    val fields: List<MoshafFieldDto> = emptyList(),
)

@Serializable
data class MoshafFieldDto(
    val key: String,
    @SerialName("name_ar") val nameAr: String? = null,
    val description: String? = null,
    



 
    val default: JsonPrimitive? = null,
    val options: List<MoshafOptionDto> = emptyList(),
)

 
@Serializable
data class MoshafOptionDto(
    val value: JsonPrimitive,
    val label: String,
)
