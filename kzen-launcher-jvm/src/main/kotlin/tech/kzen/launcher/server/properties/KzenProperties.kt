package tech.kzen.launcher.server.properties


class KzenProperties {
    //-----------------------------------------------------------------------------------------------------------------
    // NB: for some reason Map<String, Archetype> doesn't work
    var archetypes: MutableList<Archetype> = mutableListOf()


    class Archetype {
        var name: String? = null
        var title: String? = null
        var description: String? = null
        var url: String? = null
    }
}