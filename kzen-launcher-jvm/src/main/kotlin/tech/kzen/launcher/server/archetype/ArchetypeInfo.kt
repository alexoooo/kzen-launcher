package tech.kzen.launcher.server.archetype

import java.nio.file.Path


data class ArchetypeInfo(
    val title: String,
    val description: String,
    val location: Path,

    // Split archetype identity: the base name (e.g. "kzen-project") groups per-version entries, and the
    //  version (e.g. "0.29.1", "0.30.0-SNAPSHOT", or "custom" for an unparseable filename) orders them.
    //  Recorded onto a project at create/upgrade so the launcher can offer newer versions.
    val archetype: String,
    val version: String
)
