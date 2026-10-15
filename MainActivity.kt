package com.example

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.data.repository.MickRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.chat.ChatScreen
import com.example.ui.community.CommunityScreen
import com.example.ui.navigation.AppDestination
import com.example.ui.profile.ProfileScreen
import com.example.ui.sparks.SparksScreen
import com.example.ui.studio.StudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import com.example.ui.language.LocalAppLanguage
import com.example.ui.language.ProvideAppLanguage

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ProvideAppLanguage {
                MyApplicationTheme {
                    AppRootContent()
                }
            }
        }
    }
}

@Composable
fun AppRootContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isGuestMode by remember { mutableStateOf(false) }

    // Listen to Firebase auth changes
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser != null) {
                isGuestMode = false
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Attempt silent auto-sign-in on startup only if not already in guest mode
    LaunchedEffect(Unit) {
        if (currentUser == null && !isGuestMode) {
            attemptSilentSignIn(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onSuccess = {
                    currentUser = Firebase.auth.currentUser
                    isGuestMode = false
                },
                onUnauthenticated = { /* stay on auth/guest choice */ }
            )
        }
    }

    if (currentUser == null && !isGuestMode) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
                isGuestMode = false
            },
            onContinueAsGuest = {
                isGuestMode = true
            }
        )
    } else {
        val repository = remember(currentUser?.uid, isGuestMode) {
            MickRepository(context, isGuest = isGuestMode)
        }
        MainAppScaffold(
            repository = repository,
            onSignOut = {
                if (isGuestMode) {
                    isGuestMode = false
                } else {
                    Firebase.auth.signOut()
                    scope.launch {
                        try {
                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                        } catch (e: Exception) {
                            Log.e("Auth", "Failed to clear credential state", e)
                        }
                        currentUser = null
                    }
                }
            },
            onUpgradeToGoogle = {
                isGuestMode = false
            }
        )
    }
}

@Composable
fun MainAppScaffold(
    repository: MickRepository,
    onSignOut: () -> Unit,
    onUpgradeToGoogle: () -> Unit = {}
) {
    var currentDestination by remember { mutableStateOf(AppDestination.CHAT) }

    // Back handler to navigate back to Chat
    BackHandler(enabled = currentDestination != AppDestination.CHAT) {
        currentDestination = AppDestination.CHAT
    }

    Scaffold(
        bottomBar = {
            val currentLang = LocalAppLanguage.current
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_bottom_nav_bar")
            ) {
                AppDestination.entries.forEach { destination ->
                    val label = if (currentLang.code == "ur") {
                        when (destination) {
                            AppDestination.CHAT -> "بات چیت"
                            AppDestination.STUDIO -> "اسٹوڈیو"
                            AppDestination.COMMUNITY -> "لاؤنج"
                            AppDestination.SPARKS -> "خیالات"
                            AppDestination.PROFILE -> "پروفائل"
                        }
                    } else {
                        destination.title
                    }
                    NavigationBarItem(
                        selected = currentDestination == destination,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(destination.icon, contentDescription = label)
                        },
                        label = { Text(label) },
                        modifier = Modifier.testTag("nav_${destination.route}")
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Crossfade(
            targetState = currentDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "screen_crossfade"
        ) { destination ->
            when (destination) {
                AppDestination.CHAT -> ChatScreen(repository = repository)
                AppDestination.STUDIO -> StudioScreen(repository = repository)
                AppDestination.COMMUNITY -> CommunityScreen(repository = repository)
                AppDestination.SPARKS -> SparksScreen(repository = repository)
                AppDestination.PROFILE -> ProfileScreen(
                    repository = repository,
                    onSignOutClicked = onSignOut,
                    onUpgradeToGoogleClicked = onUpgradeToGoogle
                )
            }
        }
    }
}

private fun attemptSilentSignIn(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSuccess: () -> Unit,
    onUnauthenticated: () -> Unit
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}
