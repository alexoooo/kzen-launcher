package tech.kzen.launcher.server.archetype

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Maps
import org.slf4j.LoggerFactory
import tech.kzen.launcher.server.environment.LauncherEnvironment
import tech.kzen.launcher.server.properties.KzenProperties
import tech.kzen.launcher.server.service.DownloadService
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode
import tools.jackson.dataformat.yaml.YAMLFactory
import tools.jackson.dataformat.yaml.YAMLWriteFeature
import java.net.URI
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption


class ArchetypeRepo(
    private val downloadService: DownloadService,
    private val kzenProperties: KzenProperties
) {
    //-----------------------------------------------------------------------------------------------------------------
    @Suppress("ConstPropertyName")
    companion object {
        private val logger = LoggerFactory.getLogger(ArchetypeRepo::class.java)!!

        private val archetypeHome = LauncherEnvironment.projectHome
                .resolve("kzen-archetypes")

        private val archetypeMetadata = archetypeHome
                .resolve("kzen-archetypes.yaml")

        private val parser = ObjectMapper(
            YAMLFactory.builder()
                .disable(YAMLWriteFeature.SPLIT_LINES)
                .build())

        private const val titleKey = "title"
        private const val descriptionKey = "description"
        private const val locationKey = "location"

        // Downloaded to a sibling .part file and atomically moved into place, so the cached artifact
        //  only ever exists complete — a truncated download can't be mistaken for an installed archetype.
        private const val partSuffix = ".part"

        init {
            logger.info("archetypeMetadata: {}", archetypeMetadata.toAbsolutePath().normalize())
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
//    @PostConstruct
    fun init() {
        for (archetype in kzenProperties.archetypes) {
            val locationUri = URI(archetype.url!!)
            val artifactName = archetype.url!!.substringAfterLast('/')

            val archetypeInfo = ArchetypeInfo(
                    archetype.title!!,
                    archetype.description!!,
                    locate(artifactName)
            )

            val existing = read()[archetype.name]
            if (existing != null) {
                // Dev (file://) sources are mutable SNAPSHOTs — re-acquire so a rebuilt project
                //  zip is picked up. https release artifacts are immutable per *version*: keep
                //  the cached artifact only while the config still names the same file — a
                //  version bump (new artifact filename) re-acquires, so an upgraded install
                //  can't stay pinned to the previous version's template.
                val artifactUpToDate =
                    locationUri.scheme != "file" &&
                    existing.location.fileName.toString() == artifactName

                if (artifactUpToDate) {
                    // The artifact is current, but the displayed metadata (title/description,
                    //  which carries the visible version) still follows the config.
                    if (existing.title != archetypeInfo.title ||
                            existing.description != archetypeInfo.description) {
                        update(archetype.name!!, archetypeInfo)
                    }
                    continue
                }

                remove(archetype.name!!)
            }

            install(archetype.name!!, archetypeInfo, locationUri)
        }

        pruneDangling()
    }


    // A catalog entry whose cached artifact is gone can only fail at create time — drop it.
    //  Config-declared sources were just (re-)acquired above; a legacy per-version entry without
    //  its zip has nothing left to offer.
    private fun pruneDangling() {
        val current = read()

        val dangling = current.filterValues { !Files.exists(it.location) }.keys
        if (dangling.isEmpty()) {
            return
        }

        logger.info("Pruning archetypes with missing artifacts: {}", dangling)
        write(ImmutableMap.copyOf(
                Maps.filterKeys(current) { it !in dangling }))
    }


    //-----------------------------------------------------------------------------------------------------------------
    fun contains(name: String): Boolean {
        return read().keys.contains(name)
    }


    fun list(): List<String> {
        return read().keys.asList()
    }


    fun all(): ImmutableMap<String, ArchetypeInfo> {
        return read()
    }


    fun get(name: String): ArchetypeInfo {
        val current = read()

//        @Suppress("UnnecessaryVariable")
        val info = current[name]
                ?: throw IllegalArgumentException("Archetype not found: $name")

        return info
    }


//    fun read(name: String): ByteArray {
//        val info = get(name)
//        return Files.readAllBytes(info.artifact)
//    }


    fun add(name: String, artifact: ArchetypeInfo) {
        val previous = read()

        val next = ImmutableMap.builder<String, ArchetypeInfo>()
                .putAll(previous)
                .put(name, artifact)
                .build()

        write(next)
    }


    fun remove(name: String) {
        val previous = read()

        val artifact = previous[name]?.location
                ?: throw IllegalArgumentException("Archetype not found: $name")

        // The cached zip can be shared (e.g. a legacy per-version entry pointing at the same
        //  artifact as the config-declared one): only delete the file when no other entry
        //  references it.
        val shared = previous.any { it.key != name && it.value.location == artifact }
        if (!shared) {
            Files.deleteIfExists(artifact)
        }

        val next = ImmutableMap.copyOf(
                Maps.filterKeys(previous) { it != name})

        write(next)
    }


    private fun update(name: String, archetypeInfo: ArchetypeInfo) {
        val asMutable = read().toMutableMap()
        asMutable[name] = archetypeInfo
        write(ImmutableMap.copyOf(asMutable))
    }


    fun locate(artifactName: String): Path {
        return archetypeHome.resolve(artifactName)
    }


    fun install(
            name: String,
            archetypeInfo: ArchetypeInfo,
            download: URI
    ) {
        check(!contains(name)) {"Already installed: $name"}

        val target = archetypeInfo.location
        val partial = target.resolveSibling(target.fileName.toString() + partSuffix)
        downloadService.download(download, partial)
        moveIntoPlace(partial, target)

        add(name, archetypeInfo)
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
    private fun write(archetypes: Map<String, ArchetypeInfo>) {
        val asJsonValue: Map<String, Any> =
                Maps.transformValues(archetypes) { unbind(it) }

        val metadataBytes = parser.writeValueAsBytes(asJsonValue)

        if (!Files.exists(archetypeMetadata)) {
            Files.createDirectories(archetypeMetadata.toAbsolutePath().parent)
        }

        Files.write(archetypeMetadata, metadataBytes)
    }


    private fun unbind(info: ArchetypeInfo): Map<String, Any> {
        return ImmutableMap.of(
                titleKey, info.title,
                descriptionKey, info.description,
                locationKey, info.location.toAbsolutePath().normalize().toString())
    }


    //-----------------------------------------------------------------------------------------------------------------
    private fun read(): ImmutableMap<String, ArchetypeInfo> {
        if (!Files.exists(archetypeMetadata)) {
            return ImmutableMap.of()
        }

        val metadataBytes = Files.readAllBytes(archetypeMetadata)

        val metadataRoot = parser.readTree(metadataBytes)
            as? ObjectNode
            ?: throw IllegalArgumentException("Key-value map expected")

        val names = ImmutableSet.copyOf(metadataRoot.propertyNames())

        val archetypesBuilder = ImmutableMap.builder<String, ArchetypeInfo>()
        for (name in names) {
            val value = metadataRoot[name]
            val info = bindInfo(name, value)

            archetypesBuilder.put(name, info)
        }

        @Suppress("UnnecessaryVariable", "RedundantSuppression")
        val archetypes = archetypesBuilder.build()

        return archetypes
    }


    private fun bindInfo(name: String, jsonNode: JsonNode): ArchetypeInfo {
        val properties = jsonNode as? ObjectNode
                ?: throw IllegalArgumentException("Key-value map expected ($name): $jsonNode")

        val title = properties[titleKey] as? StringNode
                ?: throw IllegalStateException("Text expected ($name.$titleKey): ${properties[titleKey]}")

        val description = properties[descriptionKey] as? StringNode
                ?: throw IllegalStateException("Text expected ($name.$descriptionKey): ${properties[descriptionKey]}")

        val location = properties[locationKey] as? StringNode
                ?: throw IllegalStateException("Text expected ($name.$locationKey): ${properties[locationKey]}")

        return ArchetypeInfo(
                title.asString(),
                description.asString(),
                Paths.get(location.asString()))
    }


//    //-----------------------------------------------------------------------------------------------------------------
//    private fun initArchetypes(): ImmutableMap<String, ArchetypeInfo> {
//        return ImmutableMap.of(
//                automationZipName, ArchetypeInfo(artifact = automationZip),
//                automationJarName, ArchetypeInfo(artifact = automationJar)
//    }
}