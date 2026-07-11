package tech.kzen.launcher.server.archetype

import java.nio.file.Path


data class ArchetypeInfo(
        val title: String,
        val description: String,
        val location: Path
)