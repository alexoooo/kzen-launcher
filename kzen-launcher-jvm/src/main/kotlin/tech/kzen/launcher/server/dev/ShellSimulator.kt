package tech.kzen.launcher.server.dev

import tech.kzen.launcher.common.dto.RunningProject
import tech.kzen.launcher.common.dto.RunningState
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong


// In-memory stand-in for kzen-shell's project lifecycle, active only when the launcher runs standalone
//  in dev (see FrontendDevelopment / KzenLauncherConfig.simulateShell). It mimics the real shell's async
//  start -> running (and stop -> gone) transitions with short timers, so the running-projects UI can be
//  driven without spawning real child JVMs or running the full shell. Deliberately minimal: no real
//  processes, no persistence. Replaces RestHandler.runningProjectsDummy (which returned random noise).
class ShellSimulator {
    //-----------------------------------------------------------------------------------------------------------------
    // Per-project record. `sequence` is a monotonic start ordinal so list() can render newest-first,
    //  matching the real ProjectRegistry; it is preserved across a starting->running/failed transition.
    //  States are the client's RunningProject/RunningState DTO directly (the @SerialName values are the
    //  wire strings kzen-shell's RunningProjectStatus emits), serialized by kotlinx via ContentNegotiation.
    private class Entry(
        @Volatile var state: RunningState,
        val sequence: Long
    ) {
        @Volatile var exitCode: Int? = null
        @Volatile var recentOutput: List<String>? = null
    }


    private companion object {
        const val startMillis = 2_000L
        const val stopMillis = 1_000L
        const val runMillis = 5_000L

        // A project name containing this token resolves to FAILED instead of RUNNING, so the
        //  failed-state UI can be exercised without contriving a real spawn error.
        const val failTrigger = "fail"

        // Likewise for a child that dies after it was running.
        const val exitTrigger = "exit"

        const val simulatedExitCode = 3

        val simulatedOutput = listOf(
            "INFO  starting simulated project",
            "ERROR simulated project died unexpectedly")
    }


    //-----------------------------------------------------------------------------------------------------------------
    private val states = ConcurrentHashMap<String, Entry>()

    private val sequenceCounter = AtomicLong()

    private val scheduler: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "shell-simulator").apply { isDaemon = true }
        }


    //-----------------------------------------------------------------------------------------------------------------
    // Newest-first, matching kzen-shell's ProjectRegistry.list().
    fun list(): List<RunningProject> {
        return states.entries
            .sortedByDescending { it.value.sequence }
            .map { RunningProject(it.key, it.value.state, it.value.exitCode, it.value.recentOutput) }
    }


    // Idempotent, matching the real ProjectRegistry: a start for an already-active name is a no-op; a
    //  start for a name in a terminal state restarts it (fresh sequence, so it jumps to the top).
    fun start(name: String) {
        var created: Entry? = null
        states.compute(name) { _, existing ->
            if (existing != null && !isTerminal(existing.state)) {
                existing
            }
            else {
                Entry(RunningState.STARTING, sequenceCounter.incrementAndGet()).also { created = it }
            }
        }

        val fresh = created
            ?: return

        val target = if (name.contains(failTrigger)) RunningState.FAILED else RunningState.RUNNING
        scheduler.schedule({
            // Preserve the entry (and its sequence); only flip state, and only if still starting and
            //  still the entry we created (a stop/restart may have replaced it in the meantime).
            if (fresh.state == RunningState.STARTING && states[name] === fresh) {
                if (target == RunningState.FAILED) {
                    fresh.exitCode = simulatedExitCode
                    fresh.recentOutput = simulatedOutput
                }
                fresh.state = target
            }
        }, startMillis, TimeUnit.MILLISECONDS)

        if (name.contains(exitTrigger)) {
            scheduleSimulatedExit(name, fresh)
        }
    }


    // A child that comes up and then dies on its own, so the exited-state UI (chip, restart, crash tail)
    //  can be exercised without a real crash.
    private fun scheduleSimulatedExit(name: String, entry: Entry) {
        scheduler.schedule({
            if (entry.state == RunningState.RUNNING && states[name] === entry) {
                entry.exitCode = simulatedExitCode
                entry.recentOutput = simulatedOutput
                entry.state = RunningState.EXITED
            }
        }, startMillis + runMillis, TimeUnit.MILLISECONDS)
    }


    fun stop(name: String): Boolean {
        val entry = states[name]
            ?: return false

        // Terminal -> dismiss immediately; anything else -> brief STOPPING then gone.
        if (isTerminal(entry.state)) {
            states.remove(name, entry)
            return true
        }

        entry.state = RunningState.STOPPING
        scheduler.schedule({
            states.remove(name, entry)
        }, stopMillis, TimeUnit.MILLISECONDS)
        return true
    }


    private fun isTerminal(state: RunningState): Boolean {
        return state == RunningState.FAILED || state == RunningState.EXITED
    }
}
