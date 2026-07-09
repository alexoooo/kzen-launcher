package tech.kzen.launcher.client.state

import kotlinx.coroutines.delay
import tech.kzen.launcher.client.api.clientRestApi
import tech.kzen.launcher.client.api.launchUiAction
import tech.kzen.launcher.client.api.shellRestApi
import tech.kzen.launcher.common.dto.ArchetypeDetail
import tech.kzen.launcher.common.dto.ProjectDetail
import tech.kzen.launcher.common.dto.RunningProject


// Single source of truth for the launcher's server-backed data (archetypes / projects / running projects).
// Replaces the previous "set a ProjectLauncher state field to null so componentDidUpdate refetches" pattern
// with explicit load / invalidate calls, and lets leaf components request a refresh (after a start/stop/
// rename/…) without drilling callbacks up the component tree. Observers (the root component) are notified on
// every change so they re-render from the store.
object LauncherStore {
    private const val pollIntervalMs = 2_000L


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

    // While true, a background loop re-fetches runningProjects every pollIntervalMs so the UI reflects
    //  shell-side lifecycle transitions (starting -> running, stopping -> gone) and survives a refresh.
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


    fun invalidateProjects() {
        projects = null
        loadIfRequired()
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
    // Background polling of the shell-side lifecycle state. Started by the root component while the manage
    //  screen is mounted; stopped on unmount. This is what makes starting/stopping survive a page refresh:
    //  the state lives on the server, not in a component.
    fun startPolling() {
        if (polling) {
            return
        }
        polling = true

        launchUiAction {
            while (polling) {
                delay(pollIntervalMs)
                if (! polling) {
                    break
                }

                try {
                    publishRunning(shellRestApi.runningProjectsSilent())
                }
                catch (e: Throwable) {
                    // A transient poll failure must not kill the loop; keep polling.
                    console.error("running-projects poll failed", e)
                }
            }
        }
    }


    fun stopPolling() {
        polling = false
    }


    // Publish only when the snapshot actually changed, so an unchanged poll tick triggers no re-render.
    private fun publishRunning(latest: List<RunningProject>) {
        if (latest != runningProjects) {
            runningProjects = latest
            notifyChanged()
        }
    }
}
