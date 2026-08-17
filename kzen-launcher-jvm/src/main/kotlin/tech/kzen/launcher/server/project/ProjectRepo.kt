package tech.kzen.launcher.server.project

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Maps
import org.slf4j.LoggerFactory
import tech.kzen.launcher.common.api.CommonRestApi
import tech.kzen.launcher.common.dto.ProjectDetail
import tech.kzen.launcher.server.util.AtomicMoveUtil
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode
import tools.jackson.dataformat.yaml.YAMLFactory
import tools.jackson.dataformat.yaml.YAMLWriteFeature
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


// The user's project list, held as an immutable snapshot loaded once at construction: reads serve the
//  snapshot, mutations are serialized and each persists via a temp sibling plus an atomic move, so
//  concurrent commands can't lose an update and a crash mid-write can't leave a half-written file.
//  Consequence of load-once: the yaml stays hand-editable while the launcher is stopped (a relaunch
//  reflects the edit), but an edit made while it runs is invisible and is overwritten by the next
//  mutation. One launcher process per project home is assumed — two would last-writer-wins each other.
class ProjectRepo(projectHome: Path) {
    //-----------------------------------------------------------------------------------------------------------------
    @Suppress("ConstPropertyName")
    companion object {
        private val logger = LoggerFactory.getLogger(ProjectRepo::class.java)!!

        private const val metadataFileName = "kzen-projects.yaml"
        private const val tempSuffix = ".tmp"

        private val parser = ObjectMapper(
            YAMLFactory.builder()
                .disable(YAMLWriteFeature.SPLIT_LINES)
                .build())

        private const val homeProperty = "home"
        private const val archetypeProperty = "archetype"
        private const val versionProperty = "version"
    }


    //-----------------------------------------------------------------------------------------------------------------
    private val projectMetadata = projectHome.resolve(metadataFileName)
    private val projectMetadataTemp = projectHome.resolve(metadataFileName + tempSuffix)

    @Volatile
    private var projects: ImmutableMap<String, ProjectInfo> = readFromDisk()

    init {
        // Residue of a persist that died between the temp write and the move.
        try {
            Files.deleteIfExists(projectMetadataTemp)
        }
        catch (e: Exception) {
            logger.warn("unable to delete: {} - {}", projectMetadataTemp, e.toString())
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    fun list(): List<String> {
        return projects.keys.asList()
    }


    fun all(): ImmutableMap<String, ProjectInfo> {
        return projects
    }


    fun get(name: String): ProjectInfo {
        return projects[name]
            ?: throw IllegalArgumentException("Project not found: $name")
    }


    //-----------------------------------------------------------------------------------------------------------------
    @Synchronized
    fun add(
        name: String,
        home: Path,
        archetype: String = ProjectDetail.unknownValue,
        version: String = ProjectDetail.unknownValue
    ) {
        val info = ProjectInfo(
                home = home,
                archetype = archetype,
                version = version)

        val previous = projects

        val next = ImmutableMap.builder<String, ProjectInfo>()
                .putAll(previous)
                .put(name, info)
                .build()

        publish(next)
    }


    @Synchronized
    fun remove(name: String) {
        val previous = projects

        removeAndWrite(name, previous)
    }


    @Synchronized
    fun delete(name: String) {
        val previous = projects

        val location = previous[name]?.home
                ?: throw IllegalArgumentException("Project not found: $name")

        val locationFile = location.toFile()
        val deleted = locationFile.deleteRecursively()
        if (!deleted && locationFile.exists()) {
            // deleteRecursively() returns false and leaves files behind when one is locked — on Windows this
            //  is exactly what happens if the project is still running. Fail loudly instead of removing the
            //  list entry and orphaning the directory.
            throw IllegalStateException(
                "Could not fully delete project directory (is the project still running?): $location")
        }

        removeAndWrite(name, previous)
    }


    @Synchronized
    fun rename(name: String, newName: String) {
        val previous = projects

        val location = previous[name]?.home
                ?: throw IllegalArgumentException("Project not found: $name")

        val newLocation = location.resolveSibling(newName)

        try {
            Files.move(location, newLocation)
        }
        catch (e: IOException) {
            // On Windows a running project holds files in its directory open, so the rename fails —
            //  surface it as the state conflict it is (→ 409), matching delete above, rather than a
            //  message-less 500.
            throw IllegalStateException(
                "Could not rename project directory (is the project still running?): $location", e)
        }

        val oldInfo = previous[name]!!
        val newInfo = oldInfo.copy(home = newLocation)

        val asMutable = previous.toMutableMap()
        asMutable.remove(name)
        asMutable[newName] = newInfo
        val next = ImmutableMap.copyOf(asMutable)

        publish(next)
    }


    @Synchronized
    fun changeArguments(name: String, jvmArguments: String) {
        val previous = projects

        val previousProject = previous[name]
            ?: throw IllegalArgumentException("Project not found: $name")

        val asMutable = previous.toMutableMap()
        asMutable[name] = previousProject.copy(jvmArguments = jvmArguments)
        val next = ImmutableMap.copyOf(asMutable)

        publish(next)
    }


    // Records the archetype a project was upgraded to (after ProjectCreator.upgrade swapped its jar in), so
    //  the manage list reflects the new version and the offer recomputes. Read-copy-write like changeArguments.
    @Synchronized
    fun recordArchetype(name: String, archetype: String, version: String) {
        val previous = projects

        val previousProject = previous[name]
            ?: throw IllegalArgumentException("Project not found: $name")

        val asMutable = previous.toMutableMap()
        asMutable[name] = previousProject.copy(archetype = archetype, version = version)
        val next = ImmutableMap.copyOf(asMutable)

        publish(next)
    }


    private fun removeAndWrite(
            name: String,
            previous: ImmutableMap<String, ProjectInfo>
    ) {
        val next = ImmutableMap.copyOf(
                Maps.filterKeys(previous) { it != name})

        publish(next)
    }


    //-----------------------------------------------------------------------------------------------------------------
    // Persist before publishing: a failed write leaves memory and disk consistent at the prior state, and
    //  surfaces to the caller as the command failure it is.
    private fun publish(next: ImmutableMap<String, ProjectInfo>) {
        persist(next)
        projects = next
    }


    private fun persist(next: Map<String, ProjectInfo>) {
        val asJsonValue: Map<String, Any> =
                Maps.transformValues(next) { unbind(it) }

        val metadataBytes = parser.writeValueAsBytes(asJsonValue)

        Files.createDirectories(projectMetadata.toAbsolutePath().parent)
        Files.write(projectMetadataTemp, metadataBytes)

        AtomicMoveUtil.move(projectMetadataTemp, projectMetadata, replaceExisting = true)
    }


    private fun unbind(info: ProjectInfo): Map<String, Any> {
        return ImmutableMap.of(
            homeProperty, info.home.toAbsolutePath().normalize().toString(),
            CommonRestApi.projectJvmArgs, info.jvmArguments,
            archetypeProperty, info.archetype,
            versionProperty, info.version)
    }


    //-----------------------------------------------------------------------------------------------------------------
    private fun readFromDisk(): ImmutableMap<String, ProjectInfo> {
        if (!Files.exists(projectMetadata)) {
            return ImmutableMap.of()
        }

        try {
            val metadataBytes = Files.readAllBytes(projectMetadata)

            val metadataRoot = parser.readTree(metadataBytes)
                    as? ObjectNode
                    ?: throw IllegalArgumentException("Key-value map expected")

            val names = ImmutableSet.copyOf(metadataRoot.propertyNames())

            val projectsBuilder = ImmutableMap.builder<String, ProjectInfo>()
            for (name in names) {
                val value = metadataRoot[name]
                val info = bindInfo(name, value)

                projectsBuilder.put(name, info)
            }

            return projectsBuilder.build()
        }
        catch (e: Exception) {
            // Only a bad hand-edit can get here, and starting empty would let the next mutation persist an
            //  empty registry over the user's project list. Fail the boot instead.
            throw IllegalStateException(
                "Unable to read project registry, please correct or remove it: " +
                    "${projectMetadata.toAbsolutePath().normalize()} - ${e.message}", e)
        }
    }


    private fun bindInfo(name: String, jsonNode: JsonNode): ProjectInfo {
        val properties = jsonNode as? ObjectNode
                ?: throw IllegalArgumentException("Key-value map expected ($name): $jsonNode")

        val propertyNames = ImmutableSet.copyOf(properties.propertyNames())
        check(propertyNames.contains(homeProperty)) {"Missing property ($name): $homeProperty"}

        val path = properties[homeProperty] as? StringNode
                ?: throw IllegalStateException("Text expected ($name.$homeProperty): ${properties[homeProperty]}")

        val jvmArgs = (properties[CommonRestApi.projectJvmArgs] as? StringNode)?.asString() ?: ""

        // Additive: legacy registries (pre-SH4) and hand-edits without these keys bind to unknown.
        val archetype = (properties[archetypeProperty] as? StringNode)?.asString() ?: ProjectDetail.unknownValue
        val version = (properties[versionProperty] as? StringNode)?.asString() ?: ProjectDetail.unknownValue

        return ProjectInfo(
            Paths.get(path.asString()),
            jvmArgs,
            archetype,
            version)
    }
}
