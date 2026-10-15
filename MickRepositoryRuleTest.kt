package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MickRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun authenticatedUser_canCreateAndObserveSession() = runBlocking {
        val uid = signInTestUser("mick_creator@example.com")
        val repository = MickRepository(firestore, auth)

        val session = repository.createSession(
            title = "Sci-Fi Novel Worldbuilding",
            category = "story"
        )
        assertNotNull(session.id)
        assertEquals(uid, session.userId)
        assertEquals("Sci-Fi Novel Worldbuilding", session.title)

        val sessions = repository.observeSessions().first()
        assertTrue(sessions.any { it.id == session.id })
    }

    @Test
    fun authenticatedUser_canSaveAndObserveSparks() = runBlocking {
        val uid = signInTestUser("spark_user@example.com")
        val repository = MickRepository(firestore, auth)

        val spark = repository.saveSpark(
            title = "Bioluminescent Forest Hook",
            content = "An alien flora communicating via mycorrhizal photonic pulses.",
            category = "science",
            tags = listOf("scifi", "biology"),
            isFavorite = true
        )
        assertNotNull(spark.id)
        assertEquals(uid, spark.userId)
        assertTrue(spark.isFavorite)

        val sparks = repository.observeSparks().first()
        assertTrue(sparks.any { it.id == spark.id })
    }

    @Test
    fun authenticatedUser_canSaveAndObserveProfile() = runBlocking {
        val uid = signInTestUser("profile_user@example.com")
        val repository = MickRepository(firestore, auth)

        val profile = repository.saveUserProfile(
            username = "CreativeNovelist",
            bio = "Crafting cyber-fantasy realms",
            preferredCreativeStyle = "expansive",
            favoriteDomains = listOf("Sci-Fi", "Music", "Bio-worldbuilding")
        )
        assertNotNull(profile)
        assertEquals(uid, profile.userId)
        assertEquals("CreativeNovelist", profile.username)

        val observed = repository.observeUserProfile().first()
        assertNotNull(observed)
        assertEquals("CreativeNovelist", observed?.username)
    }

    @Test(expected = IllegalStateException::class)
    fun unauthenticatedUser_failsToCreateSession() = runBlocking {
        auth.signOut()
        val repository = MickRepository(firestore, auth)
        repository.createSession("Unauthenticated Failure", "general")
        Unit
    }

    @Test
    fun guestUser_canCreateSessionAndSaveSparksLocally() = runBlocking {
        auth.signOut()
        val repository = MickRepository(firestore, auth, isGuest = true)

        val session = repository.createSession(
            title = "Guest Brainstorming Session",
            category = "story"
        )
        assertNotNull(session.id)
        assertEquals("guest_creator", session.userId)
        assertEquals("Guest Brainstorming Session", session.title)

        val sessions = repository.observeSessions().first()
        assertTrue(sessions.any { it.id == session.id })

        val spark = repository.saveSpark(
            title = "Guest Concept Spark",
            content = "Sci-fi concept created offline as a guest.",
            category = "prompt",
            isFavorite = true
        )
        assertNotNull(spark.id)
        assertEquals("guest_creator", spark.userId)
        assertTrue(spark.isFavorite)

        val sparks = repository.observeSparks().first()
        assertTrue(sparks.any { it.id == spark.id })
    }

    @Test
    fun guestUser_canSaveAndObserveProfileLocally() = runBlocking {
        auth.signOut()
        val repository = MickRepository(firestore, auth, isGuest = true)

        val saved = repository.saveUserProfile(
            username = "GuestArtist",
            bio = "Exploring ideas",
            preferredCreativeStyle = "speculative"
        )
        assertEquals("guest_creator", saved.userId)
        assertEquals("GuestArtist", saved.username)

        val observed = repository.observeUserProfile().first()
        assertNotNull(observed)
        assertEquals("GuestArtist", observed?.username)
        assertEquals("speculative", observed?.preferredCreativeStyle)
    }

    @Test
    fun customVoice_canBeSavedAndObserved() = runBlocking {
        auth.signOut()
        val repository = MickRepository(firestore, auth, isGuest = true)

        val initialVoices = repository.observeCustomVoices().first()
        assertTrue(initialVoices.isNotEmpty())
        assertTrue(initialVoices.any { it.language.contains("Urdu") })

        val savedVoice = repository.saveCustomVoice(
            name = "Test Cloned Voice",
            description = "Cloned acoustic model",
            baseVoice = "Fenrir",
            gender = "Male",
            language = "Urdu (اردو)",
            style = "Poetic & Ghazal",
            pitch = 0.9f,
            speed = 1.0f,
            isCloned = true
        )
        assertNotNull(savedVoice.id)
        assertEquals("Test Cloned Voice", savedVoice.name)
        assertTrue(savedVoice.isCloned)

        val updatedVoices = repository.observeCustomVoices().first()
        assertTrue(updatedVoices.any { it.id == savedVoice.id })
    }
}
