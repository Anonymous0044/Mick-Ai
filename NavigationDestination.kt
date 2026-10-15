package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(val route: String, val title: String, val icon: ImageVector) {
    CHAT("chat", "Brainstorm", Icons.Default.AutoAwesome),
    STUDIO("studio", "Studio", Icons.Default.Palette),
    COMMUNITY("community", "Lounge", Icons.Default.Forum),
    SPARKS("sparks", "Sparks", Icons.Default.Bookmark),
    PROFILE("profile", "Profile", Icons.Default.Person)
}
