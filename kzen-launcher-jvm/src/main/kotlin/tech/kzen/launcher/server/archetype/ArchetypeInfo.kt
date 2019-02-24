package tech.kzen.launcher.server.archetype

import java.nio.file.Path


data class ArchetypeInfo(
        var title: String,
        var description: String,
        val location: Path
) {

//    val artifact = archetype.location!!.substringAfterLast('/')
}