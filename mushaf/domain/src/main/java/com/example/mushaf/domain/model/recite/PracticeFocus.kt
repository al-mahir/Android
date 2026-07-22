package com.example.mushaf.domain.model.recite






 
data class PracticeFocus(
    val category: MistakeCategory,
    val ruleName: String?,
    val occurrences: Int,
)
















 
fun Collection<RecitationWordFeedback>.practiceFocus(
    minimumOccurrences: Int = 2,
    limit: Int = 3,
): List<PracticeFocus> =
    asSequence()
        .filter { it.countsAsMistake }
        .flatMap { it.scorableMistakes }
        
        
        .groupingBy { it.category to it.rules.firstOrNull()?.nameArabic }
        .eachCount()
        .map { (key, count) -> PracticeFocus(key.first, key.second, count) }
        .filter { it.occurrences >= minimumOccurrences }
        .sortedWith(compareByDescending<PracticeFocus> { it.occurrences }.thenBy { it.ruleName ?: "" })
        .take(limit)
