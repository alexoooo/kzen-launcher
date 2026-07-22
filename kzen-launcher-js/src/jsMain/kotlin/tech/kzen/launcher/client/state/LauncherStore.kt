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

    // Names the user just clicked Run on that the shell hasn't reported back yet. Kept visible as a
    //  synthetic "starting" row (see mergePendingStarts) so the optimistic start survives a poll tick that
    //  races ahead of the start request, and is cleared the moment the shell's list confirms it (or the
    //  start request fails).
    private val pendingStartNames = mutableSetOf<String>()

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


    // Upgrade + refresh, returned as a Promise so the row can await completion to clear its upgrade spinner.
    //  On success the project's row shows the new recorded version; on failure (e.g. a 409 while running) it
    //  stays unchanged with the error surfaced via the interceptor.
    fun upgradeProject(name: String, type: String): Promise<Unit> = async {
        clientRestApi.upgradeProject(name, type)
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


    // Optimistic start: show the project at the top of Running as "starting" immediately — before the shell
    //  round-trips — so clicking Run feels instant. The project also leaves Available at once (it is filtered
    //  out by running-name). Only then do we fire the actual start and reconcile against the shell's
    //  authoritative list; the adaptive poll drives the eventual starting -> running. The speculative row is
    //  held (via pendingStartNames / mergePendingStarts) until the shell confirms it, so a poll tick that
    //  races ahead of the start request can't briefly erase it.
    fun startProject(project: ProjectDetail) {
        pendingStartNames.add(project.name)
        publishRunning(runningProjects ?: emptyList())

        launchUiAction {
            try {
                shellRestApi.startProject(project.name, project.path, project.jvmArgs)
            }
            catch (e: Throwable) {
                // The start request failed (already surfaced to the user via the interceptor); drop the
                //  speculative row so it doesn't linger as a phantom "starting".
                pendingStartNames.remove(project.name)
                publishRunning((runningProjects ?: emptyList()).filter { it.name != project.name })
                throw e
            }
            publishRunning(shellRestApi.runningProjects())
        }
    }


    // Restart of a project that died: the shell replaces its terminal entry with a fresh attempt. No
    //  optimistic row — the name is already in the running list, so a synthetic one would be discarded
    //  anyway; the immediate refresh plus the transitional poll cadence give feedback within a round trip.
    fun restartProject(name: String) {
        val detail = projects?.find { it.name == name }
            ?: return

        launchUiAction {
            shellRestApi.startProject(detail.name, detail.path, detail.jvmArgs)
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
                if (!polling) {
                    break
                }

                ticksSinceFetch++
                val due = isTransitioning() || ticksSinceFetch >= idleTicksPerFetch
                if (!due) {
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


    // "Something is happening": a project is mid-transition, so poll at the fast cadence. RUNNING, FAILED
    //  and EXITED are steady states (nothing pending) and fall back to the idle cadence.
    private fun isTransitioning(): Boolean {
        return runningProjects?.any {
            it.state == RunningState.STARTING || it.state == RunningState.STOPPING
        } == true
    }


    // Publish only when the snapshot actually changed, so an unchanged poll tick triggers no re-render.
    //  Any freshly-fetched list is first reconciled with still-pending optimistic starts.
    private fun publishRunning(latest: List<RunningProject>) {
        val reconciled = mergePendingStarts(latest)
        if (reconciled != runningProjects) {
            runningProjects = reconciled
            notifyChanged()
        }
    }


    // Fold not-yet-confirmed optimistic starts into a running list. A pending name the shell now reports (in
    //  any state) is confirmed, so it's dropped from the set and shown authoritatively; a pending name the
    //  shell doesn't yet know about is prepended as a synthetic "starting" row, so an in-flight start can't
    //  be transiently erased by a poll tick that raced ahead of the start request.
    private fun mergePendingStarts(latest: List<RunningProject>): List<RunningProject> {
        if (pendingStartNames.isEmpty()) {
            return latest
        }

        pendingStartNames.removeAll(latest.map { it.name }.toSet())
        if (pendingStartNames.isEmpty()) {
            return latest
        }

        return pendingStartNames.map { RunningProject(it, RunningState.STARTING) } + latest
    }


    private fun publishProjects(latest: List<ProjectDetail>) {
        if (latest != projects) {
            projects = latest
            notifyChanged()
        }
    }
}
