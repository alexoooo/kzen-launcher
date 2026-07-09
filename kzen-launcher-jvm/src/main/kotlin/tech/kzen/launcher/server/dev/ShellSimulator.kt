package tech.kzen.launcher.server.dev

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


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


    private companion object {
        const val startMillis = 2_000L
        const val stopMillis = 1_000L

        // A project name containing this token resolves to FAILED instead of RUNNING, so the
        //  failed-state UI can be exercised without contriving a real spawn error.
        const val failTrigger = "fail"
    }


    //-----------------------------------------------------------------------------------------------------------------
    private val states = ConcurrentHashMap<String, State>()

    private val scheduler: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "shell-simulator").apply { isDaemon = true }
        }


    //-----------------------------------------------------------------------------------------------------------------
    fun list(): List<Status> {
        return states.map { Status(it.key, it.value.wire) }
    }


    // Idempotent, matching the real ProjectRegistry: a start for an already-active name is a no-op; a
    //  start for a previously-FAILED name restarts it.
    fun start(name: String) {
        val previous = states.putIfAbsent(name, State.STARTING)
        if (previous != null && previous != State.FAILED) {
            return
        }
        states[name] = State.STARTING

        val target = if (name.contains(failTrigger)) State.FAILED else State.RUNNING
        scheduler.schedule({
            states.computeIfPresent(name) { _, current ->
                if (current == State.STARTING) target else current
            }
        }, startMillis, TimeUnit.MILLISECONDS)
    }


    fun stop(name: String): Boolean {
        val current = states[name]
            ?: return false

        // FAILED -> dismiss immediately; anything else -> brief STOPPING then gone.
        if (current == State.FAILED) {
            states.remove(name)
            return true
        }

        states[name] = State.STOPPING
        scheduler.schedule({
            states.remove(name)
        }, stopMillis, TimeUnit.MILLISECONDS)
        return true
    }
}
