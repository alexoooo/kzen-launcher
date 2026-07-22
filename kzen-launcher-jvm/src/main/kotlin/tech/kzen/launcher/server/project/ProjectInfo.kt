package tech.kzen.launcher.server.project

import java.nio.file.Path


data class ProjectInfo(
    val home: Path,
    val jvmArguments: String = "",

    // Archetype identity the project was last created / upgraded from, so the launcher can offer newer
    //  versions. Imported and pre-SH4 projects read as ProjectRepo.unknownValue (they adopt version
    //  tracking via one upgrade).
    val archetype: String = ProjectRepo.unknownValue,
    val version: String = ProjectRepo.unknownValue
)
