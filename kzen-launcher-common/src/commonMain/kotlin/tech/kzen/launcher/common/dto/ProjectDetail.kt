package tech.kzen.launcher.common.dto


data class ProjectDetail(
    val name: String,
    val path: String,
    val jvmArgs: String,

    val exists: Boolean
)
