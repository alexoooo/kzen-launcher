package tech.kzen.launcher.server.project

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Maps
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tech.kzen.launcher.server.environment.LauncherEnvironment
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


@Component
class ProjectRepo {
    //-----------------------------------------------------------------------------------------------------------------
    companion object {
        private val logger = LoggerFactory.getLogger(ProjectRepo::class.java)!!

        private val projectMetadata = LauncherEnvironment.projectHome
                .resolve("kzen-projects.yaml")

        private val parser = ObjectMapper(YAMLFactory())

        private const val homeProperty = "home"
    }


    //-----------------------------------------------------------------------------------------------------------------
    fun contains(name: String): Boolean {
        return read().keys.contains(name)
    }


    fun list(): List<String> {
        return read().keys.asList()
    }


    fun all(): ImmutableMap<String, ProjectInfo> {
        return read()
    }


    fun get(name: String): ProjectInfo {
        val current = read()

        @Suppress("UnnecessaryVariable")
        val info = current[name]
                ?: throw IllegalArgumentException("Archetype not found: $name")

        return info
    }


    //-----------------------------------------------------------------------------------------------------------------
    fun add(name: String, home: Path) {
        val info = ProjectInfo(
                home = home)

        val previous = read()

        val next = ImmutableMap.builder<String, ProjectInfo>()
                .putAll(previous)
                .put(name, info)
                .build()

        write(next)
    }


    fun remove(name: String) {
        val previous = read()

        removeAndWrite(name, previous)
    }


    fun delete(name: String) {
        val previous = read()

        val location = previous[name]?.home
                ?: throw IllegalArgumentException("Project not found: $name")

        location.toFile().deleteRecursively()

        removeAndWrite(name, previous)
    }


    fun rename(name: String, newName: String) {
        val previous = read()

        val location = previous[name]?.home
                ?: throw IllegalArgumentException("Project not found: $name")

        val newLocation = location.resolveSibling(newName)

        Files.move(location, newLocation)

        val oldInfo = previous[name]!!
        val newInfo = oldInfo.copy(home = newLocation)

        val asMutable= previous.toMutableMap()
        asMutable.remove(name)
        asMutable[newName] = newInfo
        val next = ImmutableMap.copyOf(asMutable)

        write(next)
    }


    private fun removeAndWrite(
            name: String,
            previous: ImmutableMap<String, ProjectInfo>
    ) {
        val next = ImmutableMap.copyOf(
                Maps.filterKeys(previous) { it != name})

        write(next)
    }


    //-----------------------------------------------------------------------------------------------------------------
    private fun write(archetypes: Map<String, ProjectInfo>) {
        val asJsonValue: Map<String, Any> =
                Maps.transformValues(archetypes) { unbind(it!!) }

        val metadataBytes = parser.writeValueAsBytes(asJsonValue)

        if (! Files.exists(projectMetadata)) {
            Files.createDirectories(projectMetadata.toAbsolutePath().parent)
        }

        Files.write(projectMetadata, metadataBytes)
    }


    private fun unbind(info: ProjectInfo): Map<String, Any> {
        return ImmutableMap.of(
                homeProperty, info.home.toAbsolutePath().normalize().toString())
    }


    //-----------------------------------------------------------------------------------------------------------------
    private fun read(): ImmutableMap<String, ProjectInfo> {
        if (! Files.exists(projectMetadata)) {
            return ImmutableMap.of()
        }

        val metadataBytes = Files.readAllBytes(projectMetadata)

        val metadataRoot = parser.readTree(metadataBytes)
                as? ObjectNode
                ?: throw IllegalArgumentException("Key-value map expected")

        val names = ImmutableSet.copyOf(metadataRoot.fieldNames())

        val archetypesBuilder = ImmutableMap.builder<String, ProjectInfo>()
        for (name in names) {
            val value = metadataRoot[name]
            val info = bindInfo(name, value)

            archetypesBuilder.put(name, info)
        }
        @Suppress("UnnecessaryVariable")
        val archetypes = archetypesBuilder.build()

        return archetypes
    }


    private fun bindInfo(name: String, jsonNode: JsonNode): ProjectInfo {
        val properties = jsonNode as? ObjectNode
                ?: throw IllegalArgumentException("Key-value map expected ($name): $jsonNode")

        val propertyNames = ImmutableSet.copyOf(properties.fieldNames())
        check(propertyNames.contains(homeProperty)) {"Missing property ($name): $homeProperty"}

        val path = properties[homeProperty] as? TextNode
                ?: throw IllegalStateException("Text expected ($name.$homeProperty): ${properties[homeProperty]}")

        return ProjectInfo(
                Paths.get(path.textValue()))
    }
}