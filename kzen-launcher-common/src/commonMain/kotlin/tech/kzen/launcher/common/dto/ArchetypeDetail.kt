package tech.kzen.launcher.common.dto

import kotlinx.serialization.Serializable


@Serializable
data class ArchetypeDetail(
    val name: String,
    val title: String,
    val description: String,
    val location: String,

    // Split identity (kotlinx defaults ⇒ additive on the wire): base name groups per-version entries,
    //  version orders them. Used client-side for the latest-per-name New Project filter and the upgrade offer.
    val archetype: String = "",
    val version: String = ""
)
