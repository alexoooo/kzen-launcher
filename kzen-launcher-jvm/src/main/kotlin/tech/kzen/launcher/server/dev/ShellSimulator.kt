package tech.kzen.launcher.server.dev

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
    enum class State(val wire: String) {
        STARTING("starting"),
        RUNNING("running"),
        STOPPING("stopping"),
        FAILED("failed")
    }

    // Wire shape mirrors kzen-shell's RunningProjectStatus and the client's RunningProject DTO:
    //  {"name": ..., "state": "starting"|"running"|"stopping"|"failed"}. Serialized by Jackson.
    data class Status(val name: String, val state: String)


    // Per-project record. `sequence` is a monotonic start ordinal so list() can render newest-first,
    //  matching the real ProjectRegistry; it is preserved across a starting->running/failed transition.
    private class Entry(
        @Volatile var state: State,
        val sequence: Long
    )


    private companion object {
        const val startMillis = 2_000L
        const val stopMillis = 1_000L

        // A project name containing this token resolves to FAILED instead of RUNNING, so the
        //  failed-state UI can be exercised without contriving a real spawn error.
        const val failTrigger = "fail"
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
    fun list(): List<Status> {
        return states.entries
            .sortedByDescending { it.value.sequence }
            .map { Status(it.key, it.value.state.wire) }
    }


    // Idempotent, matching the real ProjectRegistry: a start for an already-active name is a no-op; a
    //  start for a previously-FAILED name restarts it (fresh sequence, so it jumps to the top).
    fun start(name: String) {
        var created: Entry? = null
        states.compute(name) { _, existing ->
            if (existing != null && existing.state != State.FAILED) {
                existing
            }
            else {
                Entry(State.STARTING, sequenceCounter.incrementAndGet()).also { created = it }
            }
        }

        val fresh = created
            ?: return

        val target = if (name.contains(failTrigger)) State.FAILED else State.RUNNING
        scheduler.schedule({
            // Preserve the entry (and its sequence); only flip state, and only if still starting and
            //  still the entry we created (a stop/restart may have replaced it in the meantime).
            if (fresh.state == State.STARTING && states[name] === fresh) {
                fresh.state = target
            }
        }, startMillis, TimeUnit.MILLISECONDS)
    }


    fun stop(name: String): Boolean {
        val entry = states[name]
            ?: return false

        // FAILED -> dismiss immediately; anything else -> brief STOPPING then gone.
        if (entry.state == State.FAILED) {
            states.remove(name, entry)
            return true
        }

        entry.state = State.STOPPING
        scheduler.schedule({
            states.remove(name, entry)
        }, stopMillis, TimeUnit.MILLISECONDS)
        return true
    }
}
