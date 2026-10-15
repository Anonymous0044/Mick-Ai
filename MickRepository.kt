package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CommunityComment
import com.example.data.model.CommunityPost
import com.example.data.model.CustomVoice
import com.example.data.model.Message
import com.example.data.model.Session
import com.example.data.model.Spark
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MickRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    val isGuest: Boolean = false
) {

    constructor(context: Context, isGuest: Boolean = false) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth,
        isGuest
    )

    // --- IN-MEMORY GUEST STORE ---
    private val _guestProfile = MutableStateFlow<UserProfile?>(
        UserProfile(
            userId = "guest_creator",
            username = "Guest Creator",
            bio = "Exploring Mick AI in Guest Mode",
            preferredCreativeStyle = "poetic",
            favoriteDomains = listOf("Storytelling", "Sci-Fi", "Music")
        )
    )

    private val _guestSessions = MutableStateFlow<List<Session>>(
        listOf(
            Session(
                id = "guest_welcome_session",
                userId = "guest_creator",
                title = "Creative Brainstorming",
                category = "general",
                pinned = false,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        )
    )

    private val _guestMessages = MutableStateFlow<Map<String, List<Message>>>(
        mapOf(
            "guest_welcome_session" to listOf(
                Message(
                    id = "msg_welcome",
                    sessionId = "guest_welcome_session",
                    userId = "guest_creator",
                    role = "model",
                    text = "Hi, I'm Mick AI. What are we creating today?",
                    multimodalType = "none",
                    createdAt = Timestamp.now()
                )
            )
        )
    )

    private val _guestSparks = MutableStateFlow<List<Spark>>(
        listOf(
            Spark(
                id = "spark_sample_1",
                userId = "guest_creator",
                title = "Quantum Botany Concept",
                content = "Flora that photosynthesizes cosmic background radiation into subtle bioluminescent patterns.",
                category = "science",
                tags = listOf("sci-fi", "biology", "inspiration"),
                isFavorite = true,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        )
    )

    private val _guestCommunityPosts = MutableStateFlow<List<CommunityPost>>(
        listOf(
            CommunityPost(
                id = "guest_showcase_1",
                userId = "community_showcase",
                authorName = "Mick Studio Spotlight",
                authorAvatarUrl = "",
                title = "Prompt Engineering: Ethereal Worlds",
                content = "Tip: When generating worlds with Mick AI, specify sensory details like barometric pressure and atmospheric hues for richer descriptive writing.",
                category = "prompt",
                likesCount = 18L,
                createdAt = Timestamp.now()
            ),
            CommunityPost(
                id = "guest_showcase_2",
                userId = "community_showcase",
                authorName = "Aria Starlight",
                authorAvatarUrl = "",
                title = "Cyberpunk Neo-Noir Dialogue Hook",
                content = "\"The neon never sleeps in sector 9, but the shadows remember everything we whispered.\" Try taking this scene into the Chat tab for high-thinking variations!",
                category = "story",
                likesCount = 27L,
                createdAt = Timestamp.now()
            )
        )
    )

    private val _guestComments = MutableStateFlow<Map<String, List<CommunityComment>>>(emptyMap())

    private val _customVoices = MutableStateFlow<List<CustomVoice>>(
        listOf(
            CustomVoice(
                id = "preset_kore",
                userId = "system",
                name = "Kore (Studio Default)",
                description = "Balanced, warm, versatile tone for narration and creative ideas.",
                baseVoice = "Kore",
                gender = "Female",
                language = "English",
                style = "Warm & Expressive",
                pitch = 1.0f,
                speed = 1.0f,
                isCloned = false
            ),
            CustomVoice(
                id = "preset_aoede",
                userId = "system",
                name = "Aoede (Storyteller)",
                description = "Melodic, expressive cadence ideal for fantasy and dramatic reciting.",
                baseVoice = "Aoede",
                gender = "Female",
                language = "English",
                style = "Dramatic Storyteller",
                pitch = 1.1f,
                speed = 0.95f,
                isCloned = false
            ),
            CustomVoice(
                id = "preset_fenrir",
                userId = "system",
                name = "Fenrir (Cinematic Deep)",
                description = "Resonant, baritone cinema voice for movie trailers and deep narratives.",
                baseVoice = "Fenrir",
                gender = "Male",
                language = "English",
                style = "Cinematic / Epic",
                pitch = 0.85f,
                speed = 0.95f,
                isCloned = false
            ),
            CustomVoice(
                id = "preset_puck",
                userId = "system",
                name = "Puck (Energetic Youth)",
                description = "Fast, upbeat, playful voice for podcasting and snappy concepts.",
                baseVoice = "Puck",
                gender = "Male",
                language = "English",
                style = "Energetic / Snappy",
                pitch = 1.15f,
                speed = 1.1f,
                isCloned = false
            ),
            CustomVoice(
                id = "preset_tariq_urdu",
                userId = "system",
                name = "Tariq - طارق راوی",
                description = "Warm Urdu baritone with classical poetic cadence, ideal for ghazal and Urdu narration.",
                baseVoice = "Fenrir",
                gender = "Male",
                language = "Urdu (اردو)",
                style = "Poetic & Ghazal Reciter (شاعری و غزل)",
                pitch = 0.9f,
                speed = 0.95f,
                isCloned = false
            ),
            CustomVoice(
                id = "preset_zara_urdu",
                userId = "system",
                name = "Zara - زہرا صوتی",
                description = "Soft, melodic Urdu female voice with elegant and articulate pronunciation.",
                baseVoice = "Aoede",
                gender = "Female",
                language = "Urdu (اردو)",
                style = "Articulate & Serene (شائستہ و نرم)",
                pitch = 1.05f,
                speed = 1.0f,
                isCloned = false
            )
        )
    )

    private fun requireUserId(): String {
        if (isGuest) return "guest_creator"
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Mick AI data.")
    }

    // --- USER PROFILE & PREFERENCES ---

    fun observeUserProfile(): Flow<UserProfile?> {
        if (isGuest) return _guestProfile.asStateFlow()
        return flow {
            val uid = requireUserId()
            val path = "users/$uid"
            emitAll(
                db.collection("users").document(uid)
                    .snapshots()
                    .map { snapshot ->
                        if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null
                    }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                        throw error
                    }
            )
        }
    }

    suspend fun saveUserProfile(
        username: String,
        avatarUrl: String = "",
        bio: String = "",
        preferredCreativeStyle: String = "poetic",
        favoriteDomains: List<String> = listOf("Storytelling", "Sci-Fi", "Music"),
        enableHighThinking: Boolean = false,
        enableLowLatency: Boolean = false,
        preferredVoice: String = "Kore"
    ): UserProfile {
        if (isGuest) {
            val updated = UserProfile(
                userId = "guest_creator",
                username = username.ifBlank { "Guest Creator" },
                avatarUrl = avatarUrl,
                bio = bio,
                preferredCreativeStyle = preferredCreativeStyle,
                favoriteDomains = favoriteDomains.take(20),
                enableHighThinking = enableHighThinking,
                enableLowLatency = enableLowLatency,
                preferredVoice = preferredVoice
            )
            _guestProfile.value = updated
            return updated
        }

        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "username" to username.ifBlank { "Mick Creator" },
            "avatarUrl" to avatarUrl,
            "bio" to bio,
            "preferredCreativeStyle" to preferredCreativeStyle,
            "favoriteDomains" to favoriteDomains.take(20),
            "enableHighThinking" to enableHighThinking,
            "enableLowLatency" to enableLowLatency,
            "preferredVoice" to preferredVoice,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return UserProfile(
                userId = uid,
                username = username.ifBlank { "Mick Creator" },
                avatarUrl = avatarUrl,
                bio = bio,
                preferredCreativeStyle = preferredCreativeStyle,
                favoriteDomains = favoriteDomains,
                enableHighThinking = enableHighThinking,
                enableLowLatency = enableLowLatency,
                preferredVoice = preferredVoice
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- COMMUNITY FORUM & LOUNGE ---

    fun observeCommunityPosts(): Flow<List<CommunityPost>> {
        if (isGuest) return _guestCommunityPosts.asStateFlow()
        return flow {
            requireUserId()
            val path = "community_posts"
            emitAll(
                db.collection(path)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(CommunityPost::class.java) }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        }
    }

    suspend fun createCommunityPost(
        title: String,
        content: String,
        category: String = "general",
        mediaUrl: String? = null
    ): CommunityPost {
        if (isGuest) {
            val authorName = _guestProfile.value?.username?.ifBlank { "Guest Creator" } ?: "Guest Creator"
            val newPost = CommunityPost(
                id = "post_${UUID.randomUUID()}",
                userId = "guest_creator",
                authorName = authorName,
                authorAvatarUrl = _guestProfile.value?.avatarUrl ?: "",
                title = title.ifBlank { "Community Creative Spark" },
                content = content,
                category = category,
                mediaUrl = mediaUrl,
                likesCount = 0L,
                createdAt = Timestamp.now()
            )
            _guestCommunityPosts.value = listOf(newPost) + _guestCommunityPosts.value
            return newPost
        }

        val uid = requireUserId()
        val profile = db.collection("users").document(uid).get().await().toObject(UserProfile::class.java)
        val authorName = profile?.username?.ifBlank { "Creative Spark" } ?: auth.currentUser?.displayName?.ifBlank { "Creative Spark" } ?: "Creative Spark"
        val authorAvatar = profile?.avatarUrl ?: ""

        val docRef = db.collection("community_posts").document()
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "authorName" to authorName,
            "authorAvatarUrl" to authorAvatar,
            "title" to title.ifBlank { "Community Creative Spark" },
            "content" to content,
            "category" to category,
            "likesCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!mediaUrl.isNullOrBlank()) {
            payload["mediaUrl"] = mediaUrl
        }
        try {
            docRef.set(payload).await()
            return CommunityPost(
                id = docRef.id,
                userId = uid,
                authorName = authorName,
                authorAvatarUrl = authorAvatar,
                title = title,
                content = content,
                category = category,
                mediaUrl = mediaUrl,
                likesCount = 0L
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun likeCommunityPost(postId: String) {
        if (isGuest) {
            _guestCommunityPosts.value = _guestCommunityPosts.value.map {
                if (it.id == postId) it.copy(likesCount = it.likesCount + 1) else it
            }
            return
        }

        requireUserId()
        val docRef = db.collection("community_posts").document(postId)
        try {
            docRef.update("likesCount", FieldValue.increment(1)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeComments(postId: String): Flow<List<CommunityComment>> {
        if (isGuest) {
            return _guestComments.map { it[postId] ?: emptyList() }
        }
        return flow {
            requireUserId()
            val path = "community_posts/$postId/comments"
            emitAll(
                db.collection("community_posts").document(postId).collection("comments")
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(CommunityComment::class.java) }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        }
    }

    suspend fun addComment(postId: String, text: String): CommunityComment {
        if (isGuest) {
            val authorName = _guestProfile.value?.username?.ifBlank { "Guest Creator" } ?: "Guest Creator"
            val newComment = CommunityComment(
                id = "comm_${UUID.randomUUID()}",
                postId = postId,
                userId = "guest_creator",
                authorName = authorName,
                text = text,
                createdAt = Timestamp.now()
            )
            val existing = _guestComments.value[postId] ?: emptyList()
            _guestComments.value = _guestComments.value + (postId to (existing + newComment))
            return newComment
        }

        val uid = requireUserId()
        val profile = db.collection("users").document(uid).get().await().toObject(UserProfile::class.java)
        val authorName = profile?.username?.ifBlank { "Creator" } ?: "Creator"

        val commentsRef = db.collection("community_posts").document(postId).collection("comments")
        val docRef = commentsRef.document()
        val payload = hashMapOf<String, Any>(
            "postId" to postId,
            "userId" to uid,
            "authorName" to authorName,
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return CommunityComment(
                id = docRef.id,
                postId = postId,
                userId = uid,
                authorName = authorName,
                text = text
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- SESSIONS ---

    fun observeSessions(): Flow<List<Session>> {
        if (isGuest) return _guestSessions.asStateFlow()
        return flow {
            val uid = requireUserId()
            val path = "sessions"
            val query = db.collection(path)
                .whereEqualTo("userId", uid)

            emitAll(
                query.snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(Session::class.java).sortedByDescending { it.createdAt?.seconds ?: 0L }
                    }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        }
    }

    suspend fun createSession(title: String, category: String = "general"): Session {
        if (isGuest) {
            val newSess = Session(
                id = "sess_${UUID.randomUUID()}",
                userId = "guest_creator",
                title = title.ifBlank { "New Creative Spark" },
                category = category,
                pinned = false,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            _guestSessions.value = listOf(newSess) + _guestSessions.value
            return newSess
        }

        val uid = requireUserId()
        val docRef = db.collection("sessions").document()
        val sessionData = hashMapOf<String, Any>(
            "userId" to uid,
            "title" to title.ifBlank { "New Creative Spark" },
            "category" to category,
            "pinned" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(sessionData).await()
            return Session(
                id = docRef.id,
                userId = uid,
                title = title.ifBlank { "New Creative Spark" },
                category = category,
                pinned = false
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteSession(sessionId: String) {
        if (isGuest) {
            _guestSessions.value = _guestSessions.value.filter { it.id != sessionId }
            _guestMessages.value = _guestMessages.value - sessionId
            return
        }

        val uid = requireUserId()
        val docRef = db.collection("sessions").document(sessionId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- MESSAGES ---

    fun observeMessages(sessionId: String): Flow<List<Message>> {
        if (isGuest) {
            return _guestMessages.map { it[sessionId] ?: emptyList() }
        }
        return flow {
            requireUserId()
            val path = "sessions/$sessionId/messages"
            emitAll(
                db.collection("sessions").document(sessionId).collection("messages")
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(Message::class.java) }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        }
    }

    suspend fun addMessage(
        sessionId: String,
        role: String,
        text: String,
        multimodalType: String = "none",
        mediaUrl: String? = null
    ): Message {
        if (isGuest) {
            val newMsg = Message(
                id = "msg_${UUID.randomUUID()}",
                sessionId = sessionId,
                userId = "guest_creator",
                role = role,
                text = text,
                multimodalType = multimodalType,
                mediaUrl = mediaUrl,
                createdAt = Timestamp.now()
            )
            val currentList = _guestMessages.value[sessionId] ?: emptyList()
            _guestMessages.value = _guestMessages.value + (sessionId to (currentList + newMsg))
            _guestSessions.value = _guestSessions.value.map {
                if (it.id == sessionId) it.copy(updatedAt = Timestamp.now()) else it
            }
            return newMsg
        }

        val uid = requireUserId()
        val messagesRef = db.collection("sessions").document(sessionId).collection("messages")
        val docRef = messagesRef.document()
        val payload = hashMapOf<String, Any>(
            "sessionId" to sessionId,
            "userId" to uid,
            "role" to role,
            "text" to text,
            "multimodalType" to multimodalType,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (mediaUrl != null) {
            payload["mediaUrl"] = mediaUrl
        }
        try {
            docRef.set(payload).await()
            db.collection("sessions").document(sessionId).update("updatedAt", FieldValue.serverTimestamp())
            return Message(
                id = docRef.id,
                sessionId = sessionId,
                userId = uid,
                role = role,
                text = text,
                multimodalType = multimodalType,
                mediaUrl = mediaUrl
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- SPARKS (Saved Ideas & Notebook) ---

    fun observeSparks(): Flow<List<Spark>> {
        if (isGuest) return _guestSparks.asStateFlow()
        return flow {
            val uid = requireUserId()
            val path = "sparks"
            val query = db.collection(path)
                .whereEqualTo("userId", uid)

            emitAll(
                query.snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(Spark::class.java).sortedByDescending { it.createdAt?.seconds ?: 0L }
                    }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        throw error
                    }
            )
        }
    }

    suspend fun saveSpark(
        title: String,
        content: String,
        category: String = "prompt",
        tags: List<String> = emptyList(),
        isFavorite: Boolean = false
    ): Spark {
        if (isGuest) {
            val newSpark = Spark(
                id = "spark_${UUID.randomUUID()}",
                userId = "guest_creator",
                title = title.ifBlank { "Untitled Spark" },
                content = content,
                category = category,
                tags = tags.take(10),
                isFavorite = isFavorite,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            _guestSparks.value = listOf(newSpark) + _guestSparks.value
            return newSpark
        }

        val uid = requireUserId()
        val docRef = db.collection("sparks").document()
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "title" to title.ifBlank { "Untitled Spark" },
            "content" to content,
            "category" to category,
            "tags" to tags.take(10),
            "isFavorite" to isFavorite,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return Spark(
                id = docRef.id,
                userId = uid,
                title = title.ifBlank { "Untitled Spark" },
                content = content,
                category = category,
                tags = tags.take(10),
                isFavorite = isFavorite
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateSparkFavorite(sparkId: String, isFavorite: Boolean) {
        if (isGuest) {
            _guestSparks.value = _guestSparks.value.map {
                if (it.id == sparkId) it.copy(isFavorite = isFavorite, updatedAt = Timestamp.now()) else it
            }
            return
        }

        requireUserId()
        val docRef = db.collection("sparks").document(sparkId)
        try {
            docRef.update(
                mapOf(
                    "isFavorite" to isFavorite,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteSpark(sparkId: String) {
        if (isGuest) {
            _guestSparks.value = _guestSparks.value.filter { it.id != sparkId }
            return
        }

        requireUserId()
        val docRef = db.collection("sparks").document(sparkId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- CUSTOM & CLONED VOICES ---

    fun observeCustomVoices(): Flow<List<CustomVoice>> {
        return _customVoices.asStateFlow()
    }

    suspend fun saveCustomVoice(
        name: String,
        description: String = "",
        baseVoice: String = "Kore",
        gender: String = "Female",
        language: String = "English",
        style: String = "Warm & Expressive",
        pitch: Float = 1.0f,
        speed: Float = 1.0f,
        isCloned: Boolean = false,
        sampleAudioPath: String? = null,
        voiceBlueprint: String = ""
    ): CustomVoice {
        val uid = if (isGuest) "guest_creator" else (auth.currentUser?.uid ?: "guest_creator")
        val voiceId = "voice_${UUID.randomUUID().toString().take(8)}"
        val newVoice = CustomVoice(
            id = voiceId,
            userId = uid,
            name = name.ifBlank { if (isCloned) "Cloned Voice" else "Custom Voice" },
            description = description,
            baseVoice = baseVoice,
            gender = gender,
            language = language,
            style = style,
            pitch = pitch.coerceIn(0.5f, 2.0f),
            speed = speed.coerceIn(0.5f, 2.0f),
            isCloned = isCloned,
            sampleAudioPath = sampleAudioPath,
            voiceBlueprint = voiceBlueprint,
            createdAt = Timestamp.now()
        )
        _customVoices.value = listOf(newVoice) + _customVoices.value

        // Optionally persist to Sparks so it syncs if authenticated
        if (!isGuest && auth.currentUser != null) {
            try {
                saveSpark(
                    title = "Voice Preset: ${newVoice.name}",
                    content = "Custom Voice [${newVoice.gender}, ${newVoice.language}, Style: ${newVoice.style}, Pitch: ${newVoice.pitch}x, Speed: ${newVoice.speed}x]. Blueprint: ${newVoice.voiceBlueprint}",
                    category = if (isCloned) "voice_clone" else "custom_voice",
                    tags = listOf("voice", if (isCloned) "cloned" else "created", newVoice.language.lowercase())
                )
            } catch (_: Exception) {}
        }
        return newVoice
    }

    suspend fun deleteCustomVoice(voiceId: String) {
        _customVoices.value = _customVoices.value.filter { it.id != voiceId }
    }

    private suspend fun <T> kotlinx.coroutines.flow.FlowCollector<T>.emitAll(flow: Flow<T>) {
        flow.collect { value -> emit(value) }
    }
}
