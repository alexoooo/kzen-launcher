package tech.kzen.launcher.server.archetype

import com.google.common.collect.ImmutableMap
import org.slf4j.LoggerFactory
import tech.kzen.launcher.server.environment.LauncherEnvironment
import tech.kzen.launcher.server.service.DownloadService
import java.net.URI
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption


// The cache directory of zips IS the catalogue: every `<archetypeName>-<version>.zip` present in
//  archetypeHome is offered as a per-version entry, with the display metadata derived from the
//  configured title/description plus the version parsed from the filename. There is no metadata
//  file to drift out of sync with the config or the artifacts (the previous design kept a
//  kzen-archetypes.yaml beside the zips, reconciled incrementally per boot — every stale-dropdown
//  bug was a drift between the two).
class ArchetypeRepo(
    private val downloadService: DownloadService,
    private val archetypeName: String,
    private val title: String,
    private val descriptionBase: String,
    private val currentUrl: String,
    private val releasedUrl: String?,
    private val archetypeHome: Path = LauncherEnvironment.projectHome.resolve("kzen-archetypes")
) {
    //-----------------------------------------------------------------------------------------------------------------
    @Suppress("ConstPropertyName")
    companion object {
        private val logger = LoggerFactory.getLogger(ArchetypeRepo::class.java)!!

        private const val zipSuffix = ".zip"
        private const val snapshotSuffix = "-SNAPSHOT"

        // Downloaded to a sibling .part file and atomically moved into place, so a cached artifact
        //  only ever exists complete — a truncated download can't be mistaken for an installed archetype.
        private const val partSuffix = ".part"

        // Metadata file of the previous catalogue design, superseded by the directory scan.
        private const val legacyMetadataName = "kzen-archetypes.yaml"
    }


    //-----------------------------------------------------------------------------------------------------------------
    // Boot reconciliation. Failures degrade to serving whatever is already cached (an offline or
    //  404 boot must not kill the process — the current-version candidate legitimately 404s for a
    //  whole dev cycle when the config's GitHub URL points at the not-yet-published next release).
    fun init() {
        logger.info("archetypeHome: {}", archetypeHome.toAbsolutePath().normalize())

        // A file: source is a mutable dev snapshot — re-acquire so a rebuilt zip is picked up.
        //  An https release artifact is immutable per version — download only if absent.
        val currentArtifact = artifactName(currentUrl)
        val currentAcquired = acquire(currentUrl, reacquire = URI(currentUrl).scheme == "file")

        // The latest published release, so it is offered even where the current candidate is a
        //  dev snapshot. On a released build it names the same artifact as the current candidate,
        //  and the absence check above/below dedupes naturally.
        releasedUrl?.let { acquire(it, reacquire = false) }

        // Old snapshots are unmaintained — only the current one is offered. Skipped when the
        //  current acquisition failed: a stale snapshot the user can still create from beats
        //  deleting the only copy of it.
        if (currentAcquired) {
            pruneStaleSnapshots(currentArtifact)
        }

        cleanResidue()
    }


    private fun acquire(url: String, reacquire: Boolean): Boolean {
        val target = archetypeHome.resolve(artifactName(url))

        if (!reacquire && Files.exists(target)) {
            return true
        }

        return try {
            val partial = target.resolveSibling(target.fileName.toString() + partSuffix)
            downloadService.download(URI(url), partial)
            moveIntoPlace(partial, target)
            true
        }
        catch (e: Exception) {
            logger.error("archetype acquisition failed: {}", url, e)
            Files.exists(target)
        }
    }


    private fun pruneStaleSnapshots(currentArtifact: String) {
        val stale = scanArtifacts()
            .filter { it.endsWith(snapshotSuffix + zipSuffix) && it != currentArtifact }

        for (artifact in stale) {
            logger.info("pruning stale snapshot archetype: {}", artifact)
            deleteQuietly(archetypeHome.resolve(artifact))
        }
    }


    // Leftover .part files (from a failed download) and the legacy metadata file.
    private fun cleanResidue() {
        if (!Files.exists(archetypeHome)) {
            return
        }

        Files.list(archetypeHome).use { stream ->
            stream
                .filter { it.fileName.toString().endsWith(partSuffix) }
                .forEach { deleteQuietly(it) }
        }

        deleteQuietly(archetypeHome.resolve(legacyMetadataName))
    }


    // Prune/cleanup deletes are best-effort: another process sharing the cache dir can hold a
    //  zip open (Windows locks it), and that must not fail the boot.
    private fun deleteQuietly(path: Path) {
        try {
            Files.deleteIfExists(path)
        }
        catch (e: Exception) {
            logger.warn("unable to delete: {} - {}", path, e.toString())
        }
    }


    private fun moveIntoPlace(partial: Path, target: Path) {
        try {
            Files.move(partial, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        }
        catch (e: AtomicMoveNotSupportedException) {
            // partial and target on different stores — a plain move copies then deletes.
            logger.info("atomic move unsupported ({}), copying across stores: {} -> {}", e.message, partial, target)
            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    fun all(): ImmutableMap<String, ArchetypeInfo> {
        val sorted = scanArtifacts()
            .sortedWith(
                Comparator<String> { a, b -> compareVersions(versionOf(b), versionOf(a)) }
                    .thenBy { it })

        val builder = ImmutableMap.builder<String, ArchetypeInfo>()
        for (artifact in sorted) {
            val name = artifact.removeSuffix(zipSuffix)
            builder.put(name, ArchetypeInfo(
                title,
                "$descriptionBase - v${versionOf(artifact)}",
                archetypeHome.resolve(artifact)))
        }
        return builder.build()
    }


    // NB: lookup against the scan, never a path resolve — the name is client-supplied (the
    //  create-project `type` param) and must not become a path-traversal surface.
    fun get(name: String): ArchetypeInfo {
        return all()[name]
            ?: throw IllegalArgumentException("Archetype not found: $name")
    }


    //-----------------------------------------------------------------------------------------------------------------
    private fun scanArtifacts(): List<String> {
        if (!Files.exists(archetypeHome)) {
            return listOf()
        }

        return Files.list(archetypeHome).use { stream ->
            stream
                .filter { Files.isRegularFile(it) }
                .map { it.fileName.toString() }
                .filter { it.startsWith("$archetypeName-") && it.endsWith(zipSuffix) }
                .toList()
        }
    }


    private fun artifactName(url: String): String {
        return url.substringAfterLast('/')
    }


    private fun versionOf(artifact: String): String {
        return artifact
            .removePrefix("$archetypeName-")
            .removeSuffix(zipSuffix)
    }


    // Numeric sort key: version components plus a trailing snapshot flag, so a snapshot sorts
    //  above its equal release (the dev-current entry leads while the pair transiently coexists).
    //  An unparseable version sorts last but is still offered — never hide a cached artifact.
    private fun versionKey(version: String): List<Int> {
        val snapshot = version.endsWith(snapshotSuffix)
        val base = version.removeSuffix(snapshotSuffix)

        val components = base.split('.').map {
            it.toIntOrNull()
                ?: return listOf(Int.MIN_VALUE)
        }

        return components + (if (snapshot) 1 else 0)
    }


    private fun compareVersions(a: String, b: String): Int {
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
