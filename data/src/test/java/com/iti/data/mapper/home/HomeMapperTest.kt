package com.iti.data.mapper.home

import com.iti.data.dto.home.SheikhDto
import com.iti.data.dto.home.UserDto
import com.iti.domain.model.home.SheikhAvailability
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeMapperTest {

    @Test
    fun `two-word latin name maps to one initial per word`() {
        assertEquals("JD", UserDto(displayName = "Jamal Darwish").toDomain().initials)
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
        assertEquals("JD", UserDto(displayName = "  Jamal   Darwish  ").toDomain().initials)
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

    private fun sheikhNamed(name: String) =
        SheikhDto(id = "id", name = name, rating = 5.0, availability = "available").toDomain()

    private fun sheikhWithAvailability(token: String) =
        SheikhDto(id = "id", name = "الشيخ أحمد", rating = 5.0, availability = token).toDomain()
}
