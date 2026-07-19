package com.iti.data.mapper

import com.iti.data.dto.SheikhDto
import com.iti.data.dto.StudyCircleDto
import com.iti.data.dto.UserDto
import com.iti.domain.model.SheikhAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MapperTest {

    @Test
    fun `two-word latin name maps to one initial per word`() {
        assertEquals("JD", user(displayName = "Jamal Darwish").initials)
    }

    @Test
    fun `arabic honorific is dropped and the name gives two letters`() {
        assertEquals("أح", sheikhNamed("الشيخ أحمد").initials)
        assertEquals("عم", sheikhNamed("الشيخ عمر").initials)
    }

    @Test
    fun `name that is only an honorific yields no initials rather than crashing`() {
        assertEquals("", sheikhNamed("الشيخ").initials)
    }

    @Test
    fun `extra whitespace does not produce blank initials`() {
        assertEquals("JD", user(displayName = "  Jamal   Darwish  ").initials)
    }

    @Test
    fun `user and sheikh abbreviate the same name identically`() {
        assertEquals(user(displayName = "Omar Al-Fadl").initials, sheikhNamed("Omar Al-Fadl").initials)
    }

    @Test
    fun `known availability tokens map to their enum`() {
        assertEquals(SheikhAvailability.AVAILABLE, sheikhWithAvailability("available").availability)
        assertEquals(SheikhAvailability.IN_SESSION, sheikhWithAvailability("in_session").availability)
        assertEquals(SheikhAvailability.OFFLINE, sheikhWithAvailability("offline").availability)
    }

    @Test
    fun `unknown availability token degrades to offline`() {
        assertEquals(SheikhAvailability.OFFLINE, sheikhWithAvailability("on_holiday").availability)
    }

    @Test
    fun `study circle defaults to not joined`() {
        val circle = StudyCircleDto(id = "c1", title = "t", hostName = "h").toDomain()
        assertFalse(circle.isJoined)
        assertEquals("c1", circle.id)
    }

    private fun user(displayName: String) =
        UserDto(id = "u1", displayName = displayName).toDomain()

    private fun sheikhNamed(name: String) =
        SheikhDto(id = "id", name = name, rating = 5.0, availability = "available").toDomain()

    private fun sheikhWithAvailability(token: String) =
        SheikhDto(id = "id", name = "الشيخ أحمد", rating = 5.0, availability = token).toDomain()
}
