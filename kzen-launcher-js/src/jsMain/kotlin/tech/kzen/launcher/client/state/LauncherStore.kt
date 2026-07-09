package tech.kzen.launcher.client.state

import kotlinx.coroutines.delay
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.clientRestApi
import tech.kzen.launcher.client.api.launchUiAction
import tech.kzen.launcher.client.api.shellRestApi
import tech.kzen.launcher.common.dto.ArchetypeDetail
import tech.kzen.launcher.common.dto.ProjectDetail
import tech.kzen.launcher.common.dto.RunningProject
import tech.kzen.launcher.common.dto.RunningState
import kotlin.js.Promise


// Single source of truth for the launcher's server-backed data (archetypes / projects / running projects).
// Replaces the previous "set a ProjectLauncher state field to null so componentDidUpdate refetches" pattern
// with explicit load / invalidate calls, and lets leaf components request a refresh (after a start/stop/
// rename/…) without drilling callbacks up the component tree. Observers (the root component) are notified on
// every change so they re-render from the store.
object LauncherStore {
    // Adaptive polling: the loop wakes every fastTickMs but only fetches when due — every tick while a
    //  project is mid-transition (0.5s), otherwise every idleTicksPerFetch ticks (10 * 0.5s = 5s idle).
    //  See startPolling().
    private const val fastTickMs = 500L
    private const val idleTicksPerFetch = 10


    interface Observer {
        fun onLauncherStoreChanged()
    }


    var archetypes: List<ArchetypeDetail>? = null
        private set

    var projects: List<ProjectDetail>? = null
        private set

    var runningProjects: List<RunningProject>? = null
        private set


    // Guards against concurrent loads; a load re-checks for freshly-invalidated data when it finishes.
    private var loading: Boolean = false

    // While true, a background loop re-fetches both projects and runningProjects so the UI reflects
    //  shell-side lifecycle transitions (starting -> running, stopping -> gone), survives a refresh, and
    //  picks up projects added/deleted in another open window.
    private var polling: Boolean = false

    private val observers = mutableListOf<Observer>()


    fun subscribe(observer: Observer) {
        observers.add(observer)
    }

    fun unSubscribe(observer: Observer) {
        observers.remove(observer)
    }

    private fun notifyChanged() {
        observers.forEach { it.onLauncherStoreChanged() }
    }


    // Fetch whatever is currently missing (a null field means "stale, please (re)load"). No-op when nothing
    // is missing or a load is already running — in the latter case the in-flight load re-checks on completion,
    // so an invalidate issued mid-load is not lost.
    fun loadIfRequired() {
        if (loading) {
            return
        }

        val needArchetypes = archetypes == null
        val needProjects = projects == null
        val needRunning = runningProjects == null

        if (!(needArchetypes || needProjects || needRunning)) {
            return
        }

        loading = true
        notifyChanged()

        launchUiAction {
            try {
                if (needArchetypes) {
                    archetypes = clientRestApi.listArchetypes()
                    notifyChanged()
                }

                if (needProjects) {
                    projects = clientRestApi.listProjects()
                    notifyChanged()
                }

                if (needRunning) {
                    runningProjects = shellRestApi.runningProjects()
                    notifyChanged()
                }
            }
            finally {
                loading = false
                notifyChanged()
                // Pick up anything invalidated while this load was in flight.
                loadIfRequired()
            }
        }
    }


    // In-place (error-surfacing) refresh after an add / rename / remove / delete, so the Available card
    //  updates without flashing "Loading..." (that lets a per-item delete spinner be the only feedback).
    //  The initial population still goes through loadIfRequired(); this is only ever called post-load.
    fun invalidateProjects() {
        launchUiAction {
            publishProjects(clientRestApi.listProjects())
        }
    }


    // Delete + refresh, returned as a Promise so the row can await completion to clear its delete spinner.
    //  On success the project drops out of the list and the row unmounts; on failure it stays (with the
    //  error surfaced via the interceptor) and the spinner clears.
    fun deleteProject(name: String): Promise<Unit> = async {
        clientRestApi.deleteProject(name)
        publishProjects(clientRestApi.listProjects())
    }


    // Immediate (error-surfacing) refresh of the running list after a start/stop click, so the UI reacts
    //  without waiting for the next poll tick. Does not null the field first — the Running section updates
    //  in place rather than flashing "Loading...".
    fun invalidateRunning() {
        launchUiAction {
            publishRunning(shellRestApi.runningProjects())
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    // Background polling of the shell-side lifecycle state and the project list. Started by the root
    //  component while the manage screen is mounted; stopped on unmount. This is what makes starting/
    //  stopping survive a page refresh (state lives on the server) and what keeps two open windows in sync.
    fun startPolling() {
        if (polling) {
            return
        }
        polling = true

        launchUiAction {
            // Wake at the fast cadence but only fetch when due: every tick while a project is mid-transition
            //  (0.5s), else every idleTicksPerFetch ticks (5s). Waking often but cheaply means a starting->
            //  running flip is picked up within ~fastTickMs, without hammering the server when nothing is
            //  happening. A change made in another window is seen within 5s, then the observed transition
            //  accelerates this window to 0.5s until it settles.
            var ticksSinceFetch = idleTicksPerFetch  // fetch on the first tick
            while (polling) {
                delay(fastTickMs)
                if (! polling) {
                    break
                }

                ticksSinceFetch++
                val due = isTransitioning() || ticksSinceFetch >= idleTicksPerFetch
                if (! due) {
                    continue
                }
                ticksSinceFetch = 0

                try {
                    publishRunning(shellRestApi.runningProjectsSilent())
                    publishProjects(clientRestApi.listProjectsSilent())
                }
                catch (e: Throwable) {
                    // A transient poll failure must not kill the loop; keep polling.
                    console.error("polling failed", e)
                }
            }
        }
    }


    fun stopPolling() {
        polling = false
    }


    // "Something is happening": a project is mid-transition, so poll at the fast cadence. RUNNING and FAILED
    //  are steady states (nothing pending) and fall back to the idle cadence.
    private fun isTransitioning(): Boolean {
        return runningProjects?.any {
            it.state == RunningState.STARTING || it.state == RunningState.STOPPING
        } == true
    }


    // Publish only when the snapshot actually changed, so an unchanged poll tick triggers no re-render.
    private fun publishRunning(latest: List<RunningProject>) {
        if (latest != runningProjects) {
            runningProjects = latest
            notifyChanged()
        }
    }


    private fun publishProjects(latest: List<ProjectDetail>) {
        if (latest != projects) {
            projects = latest
            notifyChanged()
        }
    }
}
