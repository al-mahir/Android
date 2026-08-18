package com.iti.meeting.data.remote.dto

import com.iti.data.network.dto.ApiEnvelope
import com.iti.meeting.data.remote.MeetingKitJson
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Regression: the production backend returns circles keyed by `circleId`/`title`/`memberCount`
 * (not `id`/`name`/`currentMembers` as the doc contract claims). The DTO must decode both shapes
 * or every circle call fails with MissingFieldException and surfaces as a generic error. */
class CircleDtoDecodeTest {

    private val productionCircle = """
        {
          "circleId": "22a61bd9-11af-4cb4-b8c0-3f5257c4a1f5",
          "title": "جزء عم",
          "startDate": "2026-08-03T22:22:55",
          "endDate": "2026-08-03T23:22:55",
          "status": "SCHEDULED",
          "type": "PUBLIC",
          "requiresApproval": false,
          "maxParticipants": 10,
          "channelName": "circle_7ee5cfcd",
          "ownerId": "d403279a-f0dd-490f-be67-e19a08665f1a",
          "memberCount": 0
        }
    """.trimIndent()

    @Test
    fun decodesProductionShapeInsideEnvelope() {
        val envelope = MeetingKitJson.decodeFromString(
            ApiEnvelope.serializer(CircleDto.serializer()),
            """{"success":true,"data":$productionCircle}""",
        )
        val dto = envelope.data!!
        assertEquals("22a61bd9-11af-4cb4-b8c0-3f5257c4a1f5", dto.circleId)
        assertEquals("جزء عم", dto.title)
        assertEquals(10, dto.maxParticipants)
        assertEquals(0, dto.memberCount)
        assertFalse(dto.requiresApproval)
    }

    @Test
    fun decodesProductionShapeAsRawObject() {
        val dto = MeetingKitJson.decodeFromString(CircleDto.serializer(), productionCircle)
        assertEquals("22a61bd9-11af-4cb4-b8c0-3f5257c4a1f5", dto.circleId)
        assertEquals("جزء عم", dto.title)
    }

    @Test
    fun decodesDocumentedShape() {
        val dto = MeetingKitJson.decodeFromString(
            CircleDto.serializer(),
            """{"id":"c1","name":"Juz Amma","startDate":"2026-01-01T10:00:00","currentMembers":3}""",
        )
        assertEquals("c1", dto.id)
        assertEquals("Juz Amma", dto.name)
        assertEquals(3, dto.currentMembers)
    }
}
