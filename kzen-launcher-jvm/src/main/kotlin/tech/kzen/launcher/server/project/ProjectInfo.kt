package tech.kzen.launcher.server.project

import tech.kzen.launcher.common.dto.ProjectDetail
import java.nio.file.Path


data class ProjectInfo(
    val home: Path,
    val jvmArguments: String = "",

    // Archetype identity the project was last created / upgraded from, so the launcher can offer newer
    //  versions. A project with no known source reads as ProjectDetail.unknownValue (it adopts version
    //  tracking via one upgrade).
    val archetype: String = ProjectDetail.unknownValue,
    val version: String = ProjectDetail.unknownValue
)
