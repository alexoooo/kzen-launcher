package tech.kzen.launcher.common.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ProjectDetail(
    val name: String,
    val path: String,

    // The server emits this under the JSON key `args` (see RestHandler.listProjects).
    @SerialName("args")
    val jvmArgs: String,

    val exists: Boolean
)
