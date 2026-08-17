package tech.kzen.launcher.common.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ProjectDetail(
    val name: String,
    val path: String,

    // Both sides serialize this DTO with kotlinx, so the annotation is the single source of the
    //  wire name `args` (kept from the pre-kotlinx server encoding).
    @SerialName("args")
    val jvmArgs: String,

    val exists: Boolean,

    // Archetype the project was last created / upgraded from (kotlinx defaults ⇒ additive on the wire).
    //  Drives the per-row Upgrade offer and the shown version.
    val archetype: String = unknownValue,
    val version: String = unknownValue
) {
    @Suppress("ConstPropertyName")
    companion object {
        // Archetype/version of a project with no known archetype source (an import, or a registry entry
        //  predating version tracking). Such a project is offered every cached version as an upgrade, so
        //  one upgrade lets it adopt tracking. Written by the server registry, read by the client.
        const val unknownValue = "unknown"
    }
}
