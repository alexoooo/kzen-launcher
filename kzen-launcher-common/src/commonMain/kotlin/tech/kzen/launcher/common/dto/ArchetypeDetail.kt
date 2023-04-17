package tech.kzen.launcher.common.dto


data class ArchetypeDetail(
    val name: String,
    var title: String,
    var description: String,
    val location: String
) {
    companion object {
        fun fromCollection(collection: Map<String, String>): ArchetypeDetail {
            return ArchetypeDetail(
                    collection["name"]!!,
                    collection["title"]!!,
                    collection["description"]!!,
                    collection["location"]!!)
        }
    }
}