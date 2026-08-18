package com.iti.meeting.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression: every circle list endpoint returns a Spring `Page` envelope (`data.content: [...]`),
 * not a bare array. `decodeList` must unwrap it or every list decodes as empty. */
class CircleApiPageDecodeTest {

    private val pageBody = """
        {"success":true,"message":"Circles retrieved successfully","data":{
          "content":[
            {"circleId":"22a61bd9-11af-4cb4-b8c0-3f5257c4a1f5","title":"جزء عم","startDate":"2026-08-03T22:22:55","endDate":"2026-08-03T23:22:55","status":"SCHEDULED","type":"PUBLIC","requiresApproval":false,"maxParticipants":10,"channelName":"circle_7ee5cfcd","ownerId":"d403279a-f0dd-490f-be67-e19a08665f1a","memberCount":0},
            {"circleId":"9fec8d49-0a7c-4164-ba10-333c7a2b9ec3","title":"test","startDate":"2026-08-05T05:41:39","endDate":"2026-08-05T06:41:39","status":"ONGOING","type":"PUBLIC","requiresApproval":true,"maxParticipants":10,"channelName":"circle_ad023abf","ownerId":"d403279a-f0dd-490f-be67-e19a08665f1a","memberCount":0}
          ],
          "empty":false,"first":true,"last":true,"number":0,"numberOfElements":2,"pageable":{"offset":0,"pageNumber":0,"pageSize":20,"paged":true},"size":20,"totalElements":2,"totalPages":1
        }}
    """.trimIndent()

    @Test
    fun `getPublicCircles decodes the page content instead of an empty list`() = runBlocking {
        val engine = MockEngine {
            respond(pageBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val api = CircleApi(HttpClient(engine))

        val circles = api.getPublicCircles()

        assertEquals(2, circles.size)
        assertEquals("22a61bd9-11af-4cb4-b8c0-3f5257c4a1f5", circles[0].circleId)
        assertEquals("جزء عم", circles[0].title)
        assertEquals("ONGOING", circles[1].status)
    }

    @Test
    fun `getPublicCircles decodes an empty page`() = runBlocking {
        val engine = MockEngine {
            respond(
                """{"success":true,"data":{"content":[],"empty":true,"numberOfElements":0,"totalElements":0}}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = CircleApi(HttpClient(engine))

        assertTrue(api.getPublicCircles().isEmpty())
    }
}
