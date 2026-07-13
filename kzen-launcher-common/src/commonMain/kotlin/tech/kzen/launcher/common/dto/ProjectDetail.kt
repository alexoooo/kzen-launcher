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

    val exists: Boolean
)
