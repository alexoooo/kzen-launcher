package tech.kzen.launcher.client.components

import kotlinx.css.Color
import kotlinx.css.em
import kotlinx.css.margin
import react.*
import styled.styledDiv
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.clientRestApi
import tech.kzen.launcher.client.api.shellRestApi
import tech.kzen.launcher.client.wrap.MaterialCard
import tech.kzen.launcher.client.wrap.MaterialCardContent
import tech.kzen.launcher.client.wrap.MaterialDivider
import tech.kzen.launcher.client.wrap.reactStyle
import tech.kzen.launcher.common.dto.ProjectDetail


class ProjectLauncher(
        props: ProjectLauncher.Props
): RComponent<ProjectLauncher.Props, ProjectLauncher.State>(props) {
    //-----------------------------------------------------------------------------------------------------------------
    class Props(

    ) : RProps

    class State(
            var artifacts: Map<String, String>?,
            var projects: List<ProjectDetail>?,
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
//            console.log("$$ artifacts: $response")

            setState {
                artifacts = response
            }
        }

        if (needProjects) {
            val response = clientRestApi.listProjects()
//            console.log("$$ projects: $response")

            setState {
                projects = response
            }
        }

        if (needRunningProjects) {
            val response = shellRestApi.runningProjects()
//            console.log("$$ running: $response")

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
        styledDiv {
            child(MaterialCard::class) {
                attrs {
                    style = reactStyle {
                        backgroundColor = Color.white
                        margin(1.em)
                    }

                    raised = true
                }


                child(MaterialCardContent::class) {
                    child(ProjectRunning::class) {
                        attrs.projects = state.runningProjects
                        attrs.didStop = {
                            state.runningProjects = null
                            loadFromServerIfRequired()
                        }
                    }
                }

                child(MaterialDivider::class) {}

                child(MaterialCardContent::class) {
                    child(ProjectCreate::class) {
                        attrs.artifacts = state.artifacts
                        attrs.didCreate = {
                            state.projects = null
                            loadFromServerIfRequired()
                        }
                    }
                }

                child(MaterialDivider::class) {}

                child(MaterialCardContent::class) {
                    child(ProjectList::class) {
                        attrs.projects = state.projects
                                ?.filter{ ! (state.runningProjects?.contains(it.name) ?: false) }

                        attrs.didStart = {
                            state.runningProjects = null
                            loadFromServerIfRequired()
                        }

                        attrs.didRemove = {
                            state.projects = null
                            loadFromServerIfRequired()
                        }

                        attrs.didDelete = {
                            state.projects = null
                            loadFromServerIfRequired()
                        }
                    }
                }
            }
        }
    }
}