package tech.kzen.launcher.server.archetype

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Maps
import com.google.common.io.ByteStreams
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tech.kzen.launcher.server.environment.LauncherEnvironment
import tech.kzen.launcher.server.service.DownloadService
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.annotation.PostConstruct


@Component
class ArchetypeRepo(
        private val downloadService: DownloadService
) {
    //-----------------------------------------------------------------------------------------------------------------
    companion object {
        private val logger = LoggerFactory.getLogger(ArchetypeRepo::class.java)!!

        private val archetypeHome = LauncherEnvironment.projectHome
                .resolve("kzen-archetypes")

        private val archetypeMetadata = archetypeHome
                .resolve("kzen-archetypes.yaml")

        private val parser = ObjectMapper(YAMLFactory())

        private const val pathProperty = "path"


        // TODO: read artifacts from resource, or lookup dynamically
        private const val projectVersion = "0.3.2"

        private const val artifactPrefix =
            "https://github.com/alexoooo/kzen-project/releases/download/v$projectVersion"

        private const val automationZipName = "automation-zip"
        private const val automationZipArtifact = "kzen-project-$projectVersion.zip"
        private val automationZipLocation = URI("$artifactPrefix/$automationZipArtifact")

        private const val automationJarName = "automation-jar"
        private const val automationJarArtifact = "kzen-project-jvm-$projectVersion.jar"
        private val automationJarLocation = URI("$artifactPrefix/$automationJarArtifact")
    }


    //-----------------------------------------------------------------------------------------------------------------
    @PostConstruct
    fun init() {
        val initial = read()

        if (! initial.containsKey(automationJarName)) {
            install(automationJarName, automationJarArtifact, automationJarLocation)
        }

        if (! initial.containsKey(automationZipName)) {
            install(automationZipName, automationZipArtifact, automationZipLocation)
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


    fun add(name: String, root: Path) {
        val info = ArchetypeInfo(
                artifact = root)

        val previous = read()

        val next = ImmutableMap.builder<String, ArchetypeInfo>()
                .putAll(previous)
                .put(name, info)
                .build()

        write(next)
    }


    fun remove(name: String) {
        val previous = read()

        val artifact = previous[name]?.artifact
                ?: throw IllegalArgumentException("Archetype not found: $name")

        Files.deleteIfExists(artifact)

        val next = ImmutableMap.copyOf(
                Maps.filterKeys(previous) { it != name})

        write(next)
    }


    fun install(name: String, artifact: String, download: URI) {
        check(! contains(name)) {"Already installed: $name"}

        val downloadBytes = downloadService.download(download)

        val destination = archetypeHome.resolve(artifact)

        Files.createDirectories(destination.parent)
        Files.write(destination, downloadBytes)

        add(name, destination)
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
                pathProperty, info.artifact.toAbsolutePath().normalize().toString())
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

        val propertyNames = ImmutableSet.copyOf(properties.fieldNames())
        check(propertyNames.contains(pathProperty), {"Missing property ($name): $pathProperty"})

        val path = properties[pathProperty] as? TextNode
                ?: throw IllegalStateException("Text expected ($name.$pathProperty): ${properties[pathProperty]}")

        return ArchetypeInfo(
                Paths.get(path.textValue()))
    }


//    //-----------------------------------------------------------------------------------------------------------------
//    private fun initArchetypes(): ImmutableMap<String, ArchetypeInfo> {
//        return ImmutableMap.of(
//                automationZipName, ArchetypeInfo(artifact = automationZip),
//                automationJarName, ArchetypeInfo(artifact = automationJar)
//    }
}