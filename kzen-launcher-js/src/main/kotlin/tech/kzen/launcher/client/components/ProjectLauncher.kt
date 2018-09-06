package tech.kzen.launcher.client.components

import react.*
import react.dom.br
import react.dom.div
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.clientRestApi
import tech.kzen.launcher.client.api.shellRestApi


class ProjectLauncher(
        props: ProjectLauncher.Props
): RComponent<ProjectLauncher.Props, ProjectLauncher.State>(props) {
    //-----------------------------------------------------------------------------------------------------------------
    class Props(

    ) : RProps

    class State(
            var artifacts: Map<String, String>?,
            var projects: Map<String, String>?,
            var runningProjects: List<String>?,
            var loading: Boolean = false
    ) : RState


    //-----------------------------------------------------------------------------------------------------------------
    override fun componentDidMount() {
        loadFromServerIfRequired()
    }


    override fun componentDidUpdate(prevProps: Props, prevState: State, snapshot: Any) {
        loadFromServerIfRequired()
    }


    private fun loadFromServerIfRequired() {
        if (state.loading) {
            return
        }

        async {
            loadFromServerAsync()
        }
    }

    private suspend fun loadFromServerAsync() {
        val needArtifacts = (state.artifacts == null)
        val needProjects = (state.projects == null)
        val needRunningProjects = (state.runningProjects == null)

        if (! (needArtifacts || needProjects || needRunningProjects)) {
            return
        }

        setState {
            loading = true
        }

        if (needArtifacts) {
            val response = clientRestApi.listArtifacts()
            console.log("$$ artifacts: $response")

            setState {
                artifacts = response
            }
        }

        if (needProjects) {
            val response = clientRestApi.listProjects()
            console.log("$$ projects: $response")

            setState {
                projects = response
            }
        }

        if (needRunningProjects) {
            val response = shellRestApi.runningProjects()
            console.log("$$ running: $response")

            setState {
                runningProjects = response
            }
        }


        setState {
            loading = false
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun RBuilder.render() {
        div {
            child(ProjectRunning::class) {
                attrs.projects = state.runningProjects
                attrs.didStop = {
                    state.runningProjects = null
                    loadFromServerIfRequired()
                }
            }
        }

        br {}

        div {
            child(ProjectCreate::class) {
                attrs.artifacts = state.artifacts
                attrs.didCreate = {
                    state.projects = null
                    loadFromServerIfRequired()
                }
            }
        }

        br {}

        div {
            child(ProjectList::class) {
                attrs.projects = state.projects
                        ?.filterKeys { ! (state.runningProjects?.contains(it) ?: false) }

                attrs.didStart = {
                    state.runningProjects = null
                    loadFromServerIfRequired()
                }
            }
        }
    }
}