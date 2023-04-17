package tech.kzen.launcher.server.project

import java.nio.file.Path


data class ProjectInfo(
    val home: Path,
    val jvmArguments: String = ""
)