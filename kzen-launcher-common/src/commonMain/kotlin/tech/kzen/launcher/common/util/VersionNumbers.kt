package tech.kzen.launcher.common.util


// Version ordering shared by the server (archetype catalogue sort) and the client (upgrade-offer gating,
//  dialog ordering, downgrade warning). Extracted from ArchetypeRepo so both sides order versions the same
//  way — a client that disagreed with the server's sort would offer "newer" versions that aren't.
object VersionNumbers {
    @Suppress("ConstPropertyName")
    private const val snapshotSuffix = "-SNAPSHOT"


    // Numeric sort key: version components plus a trailing snapshot flag, so a snapshot sorts above its
    //  equal release (the dev-current entry leads while the pair transiently coexists). An unparseable
    //  version sorts last but is still offered — never hide a cached artifact.
    private fun versionKey(version: String): List<Int> {
        val snapshot = version.endsWith(snapshotSuffix)
        val base = version.removeSuffix(snapshotSuffix)

        val components = base.split('.').map {
            it.toIntOrNull()
                ?: return listOf(Int.MIN_VALUE)
        }

        return components + (if (snapshot) 1 else 0)
    }


    fun parses(version: String): Boolean {
        return versionKey(version) != listOf(Int.MIN_VALUE)
    }


    fun isSnapshot(version: String): Boolean {
        return version.endsWith(snapshotSuffix)
    }


    fun compare(a: String, b: String): Int {
        val aKey = versionKey(a)
        val bKey = versionKey(b)

        for (i in 0 until maxOf(aKey.size, bKey.size)) {
            val comparison = aKey.getOrElse(i) { 0 }.compareTo(bKey.getOrElse(i) { 0 })
            if (comparison != 0) {
                return comparison
            }
        }
        return 0
    }
}
