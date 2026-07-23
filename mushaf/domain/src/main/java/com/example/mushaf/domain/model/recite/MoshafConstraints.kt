package com.example.mushaf.domain.model.recite


object MoshafConstraints {

    const val MADD_AARED = "madd_aared_len"
    const val MADD_ALLEEN = "madd_alleen_len"


    val EFFECTIVE_SERVER_DEFAULTS: Map<String, MoshafValue> = mapOf(
        "madd_monfasel_len" to MoshafValue.Number(4),
        MADD_AARED to MoshafValue.Number(4),
        MADD_ALLEEN to MoshafValue.Number(4),
    )

    fun apply(
        current: Map<String, MoshafValue>,
        key: String,
        value: MoshafValue,
    ): Map<String, MoshafValue> {
        val updated = current + (key to value)
        val aared = updated.number(MADD_AARED) ?: return updated
        val alleen = updated.number(MADD_ALLEEN) ?: return updated
        if (alleen <= aared) return updated

        return when (key) {
            MADD_AARED -> updated + (MADD_ALLEEN to MoshafValue.Number(aared))
            MADD_ALLEEN -> updated + (MADD_AARED to MoshafValue.Number(alleen))
            else -> updated
        }
    }


    fun resolveForWire(selected: Map<String, MoshafValue>): Map<String, MoshafValue> {
        val aared = selected.number(MADD_AARED) ?: return selected
        val effectiveAlleen = selected.number(MADD_ALLEEN)
            ?: (EFFECTIVE_SERVER_DEFAULTS[MADD_ALLEEN] as? MoshafValue.Number)?.value
            ?: return selected
        if (effectiveAlleen <= aared) return selected
        return selected + (MADD_ALLEEN to MoshafValue.Number(aared))
    }

    private fun Map<String, MoshafValue>.number(key: String): Int? =
        (this[key] as? MoshafValue.Number)?.value
}
