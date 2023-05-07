package tech.kzen.launcher.server.archetype

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Maps
import org.slf4j.LoggerFactory
//import org.springframework.stereotype.Component
import tech.kzen.launcher.server.environment.LauncherEnvironment
import tech.kzen.launcher.server.properties.KzenProperties
import tech.kzen.launcher.server.service.DownloadService
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


class ArchetypeRepo(
    private val downloadService: DownloadService,
    private val kzenProperties: KzenProperties
) {
    //-----------------------------------------------------------------------------------------------------------------
    companion object {
        private val logger = LoggerFactory.getLogger(ArchetypeRepo::class.java)!!

        private val archetypeHome = LauncherEnvironment.projectHome
                .resolve("kzen-archetypes")

        private val archetypeMetadata = archetypeHome
                .resolve("kzen-archetypes.yaml")

        private val parser = ObjectMapper(
            YAMLFactory()
                .disable(YAMLGenerator.Feature.SPLIT_LINES))

        private const val titleKey = "title"
        private const val descriptionKey = "description"
        private const val locationKey = "location"
    }


    //-----------------------------------------------------------------------------------------------------------------
//    @PostConstruct
    fun init() {
        val initial = read()

        for (archetype in kzenProperties.archetypes) {
            if (! initial.containsKey(archetype.name)) {
                val artifactName = archetype.url!!.substringAfterLast('/')
                val locationUri = URI(archetype.url!!)

                val archetypeInfo = ArchetypeInfo(
                        archetype.title!!,
                        archetype.description!!,
                        locate(artifactName)
                )

                install(archetype.name!!, archetypeInfo, locationUri)
            }
        }
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

        @Suppress("UnnecessaryVariable")
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

        Files.deleteIfExists(artifact)

        val next = ImmutableMap.copyOf(
                Maps.filterKeys(previous) { it != name})

        write(next)
    }


    fun locate(artifactName: String): Path {
        return archetypeHome.resolve(artifactName)
    }


    fun install(
            name: String,
            archetypeInfo: ArchetypeInfo,
            download: URI
    ) {
        check(! contains(name)) {"Already installed: $name"}

        downloadService.download(download, archetypeInfo.location)

        add(name, archetypeInfo)
    }


    //-----------------------------------------------------------------------------------------------------------------
    private fun write(archetypes: Map<String, ArchetypeInfo>) {
        val asJsonValue: Map<String, Any> =
                Maps.transformValues(archetypes) { unbind(it!!) }

        val metadataBytes = parser.writeValueAsBytes(asJsonValue)

        if (! Files.exists(archetypeMetadata)) {
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
        if (! Files.exists(archetypeMetadata)) {
            return ImmutableMap.of()
        }

        val metadataBytes = Files.readAllBytes(archetypeMetadata)

        val metadataRoot = parser.readTree(metadataBytes)
            as? ObjectNode
            ?: throw IllegalArgumentException("Key-value map expected")

        val names = ImmutableSet.copyOf(metadataRoot.fieldNames())

        val archetypesBuilder = ImmutableMap.builder<String, ArchetypeInfo>()
        for (name in names) {
            val value = metadataRoot[name]
            val info = bindInfo(name, value)

            archetypesBuilder.put(name, info)
        }
        @Suppress("UnnecessaryVariable")
        val archetypes = archetypesBuilder.build()

        return archetypes
    }


    private fun bindInfo(name: String, jsonNode: JsonNode): ArchetypeInfo {
        val properties = jsonNode as? ObjectNode
                ?: throw IllegalArgumentException("Key-value map expected ($name): $jsonNode")

        val title = properties[titleKey] as? TextNode
                ?: throw IllegalStateException("Text expected ($name.$titleKey): ${properties[titleKey]}")

        val description = properties[descriptionKey] as? TextNode
                ?: throw IllegalStateException("Text expected ($name.$descriptionKey): ${properties[descriptionKey]}")

        val location = properties[locationKey] as? TextNode
                ?: throw IllegalStateException("Text expected ($name.$locationKey): ${properties[locationKey]}")

        return ArchetypeInfo(
                title.textValue(),
                description.textValue(),
                Paths.get(location.textValue()))
    }


//    //-----------------------------------------------------------------------------------------------------------------
//    private fun initArchetypes(): ImmutableMap<String, ArchetypeInfo> {
//        return ImmutableMap.of(
//                automationZipName, ArchetypeInfo(artifact = automationZip),
//                automationJarName, ArchetypeInfo(artifact = automationJar)
//    }
}