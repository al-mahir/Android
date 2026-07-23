package com.example.mushaf.domain.model.recite

import org.junit.Assert.assertEquals
import org.junit.Test





 
class MoshafConstraintsTest {

    @Test
    fun `lowering aared pulls leen down with it`() {
        val result = MoshafConstraints.apply(
            current = mapOf(MoshafConstraints.MADD_ALLEEN to MoshafValue.Number(6)),
            key = MoshafConstraints.MADD_AARED,
            value = MoshafValue.Number(2),
        )

        
        
        assertEquals(MoshafValue.Number(2), result[MoshafConstraints.MADD_AARED])
        assertEquals(MoshafValue.Number(2), result[MoshafConstraints.MADD_ALLEEN])
    }

    @Test
    fun `raising leen pushes aared up to meet it`() {
        val result = MoshafConstraints.apply(
            current = mapOf(MoshafConstraints.MADD_AARED to MoshafValue.Number(2)),
            key = MoshafConstraints.MADD_ALLEEN,
            value = MoshafValue.Number(6),
        )

        assertEquals(MoshafValue.Number(6), result[MoshafConstraints.MADD_ALLEEN])
        assertEquals(MoshafValue.Number(6), result[MoshafConstraints.MADD_AARED])
    }

    @Test
    fun `a valid pair is left alone`() {
        val result = MoshafConstraints.apply(
            current = mapOf(MoshafConstraints.MADD_AARED to MoshafValue.Number(6)),
            key = MoshafConstraints.MADD_ALLEEN,
            value = MoshafValue.Number(4),
        )

        assertEquals(MoshafValue.Number(6), result[MoshafConstraints.MADD_AARED])
        assertEquals(MoshafValue.Number(4), result[MoshafConstraints.MADD_ALLEEN])
    }

    @Test
    fun `an unrelated field is untouched`() {
        val result = MoshafConstraints.apply(
            current = emptyMap(),
            key = "madd_monfasel_len",
            value = MoshafValue.Number(5),
        )

        assertEquals(mapOf("madd_monfasel_len" to MoshafValue.Number(5)), result)
    }

    @Test
    fun `lowering aared alone still sends the leen the server would have resolved`() {
        val forWire = MoshafConstraints.resolveForWire(
            mapOf(MoshafConstraints.MADD_AARED to MoshafValue.Number(2)),
        )

        
        
        assertEquals(MoshafValue.Number(2), forWire[MoshafConstraints.MADD_ALLEEN])
    }

    @Test
    fun `a selection that needs no help is passed through unchanged`() {
        val selection = mapOf("madd_monfasel_len" to MoshafValue.Number(4))

        assertEquals(selection, MoshafConstraints.resolveForWire(selection))
    }
}
