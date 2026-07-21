package tech.kzen.launcher.common.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


// One entry in the container shell's running-projects list (GET /shell/project). The shell (or the dev
//  simulator) is the source of truth for the lifecycle state; the client polls this and renders each
//  project according to its state. See CommonRestApi.shellProject.
@Serializable
data class RunningProject(
    val name: String,
    val state: RunningState,

    // Set when the child died on its own — after it was running (EXITED) or during boot (FAILED).
    val exitCode: Int? = null,

    // Tail of the child's output, populated for the FAILED and EXITED states.
    val recentOutput: List<String>? = null
)


@Serializable
enum class RunningState {
    // Spawn requested; child booting, not yet serving HTTP.
    @SerialName("starting")
    STARTING,

    // Child is up and reverse-proxyable.
    @SerialName("running")
    RUNNING,

    // Stop requested; child being killed.
    @SerialName("stopping")
    STOPPING,

    // Start failed (spawn error, child died during boot, or readiness timeout). Dismissed via stop.
    @SerialName("failed")
    FAILED,

    // Child died on its own after it was running. Terminal: restarted by starting it again, or dismissed
    //  via stop.
    @SerialName("exited")
    EXITED
}
