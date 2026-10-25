package com.example.ritmo.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Unit tests for the plain-Kotlin helpers in RitmoUtils.kt.
// These run on the JVM (no phone/emulator needed), which is what lets
// GitHub Actions run them automatically on every push.
class RitmoUtilsTest {

    // --- roomIdFor ---

    @Test
    fun roomIdFor_turnsRouteAndTimeIntoASlug() {
        assertEquals(
            "route-4-wynberg-to-rosebank_07-00-07-30",
            roomIdFor("Route 4 - Wynberg to Rosebank", "07:00-07:30")
        )
    }

    @Test
    fun roomIdFor_isCaseInsensitive() {
        assertEquals(
            roomIdFor("Century City", "08:00"),
            roomIdFor("century city", "08:00")
        )
    }

    @Test
    fun roomIdFor_ignoresExtraSpacesAndPunctuation() {
        assertEquals("century-city_8-00", roomIdFor("  Century   City!! ", "8:00"))
    }

    // --- conversationIdFor ---

    @Test
    fun conversationIdFor_sameIdRegardlessOfWhoOpensTheChat() {
        assertEquals(conversationIdFor("a", "b"), conversationIdFor("b", "a"))
    }

    @Test
    fun conversationIdFor_sortsAndJoinsTheTwoIds() {
        assertEquals("a_b", conversationIdFor("b", "a"))
    }

    // --- hoursUntilExpiry (posts disappear after 24 hours) ---

    @Test
    fun hoursUntilExpiry_freshPostHasAboutADayLeft() {
        val hours = hoursUntilExpiry(System.currentTimeMillis())
        assertTrue(hours > 23.9 && hours <= 24.0)
    }

    @Test
    fun hoursUntilExpiry_twelveHourOldPostHasAboutTwelveLeft() {
        val twelveHoursAgo = System.currentTimeMillis() - 12L * 60 * 60 * 1000
        assertEquals(12.0, hoursUntilExpiry(twelveHoursAgo), 0.01)
    }

    @Test
    fun hoursUntilExpiry_oldPostIsExpired() {
        val twentyFiveHoursAgo = System.currentTimeMillis() - 25L * 60 * 60 * 1000
        assertTrue(hoursUntilExpiry(twentyFiveHoursAgo) < 0)
    }

    // --- greetingForNow ---

    @Test
    fun greetingForNow_isOneOfTheThreeGreetings() {
        val allowed = setOf("Good morning", "Good afternoon", "Good evening")
        assertTrue(greetingForNow() in allowed)
    }
}