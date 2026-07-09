package tech.kzen.launcher.common.dto

import kotlinx.serialization.Serializable


@Serializable
data class ArchetypeDetail(
    val name: String,
    var title: String,
    var description: String,
    val location: String
)
