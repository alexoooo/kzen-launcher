package tech.kzen.launcher.client.components

import kotlinx.css.*
import kotlinx.html.title
import react.*
import react.dom.attrs
import styled.*
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.clientRestApi
import tech.kzen.launcher.client.api.shellRestApi
import tech.kzen.launcher.client.components.add.NewProjectScreen
import tech.kzen.launcher.client.components.manage.ManageProjectsScreen
import tech.kzen.launcher.client.service.ErrorBus
import tech.kzen.launcher.client.wrap.*
import tech.kzen.launcher.common.dto.ArchetypeDetail
import tech.kzen.launcher.common.dto.ProjectDetail


class ProjectLauncher(
        props: Props
):
        RComponent<ProjectLauncher.Props, ProjectLauncher.State>(props),
        ErrorBus.Subscriber
{
    //-----------------------------------------------------------------------------------------------------------------
    interface Props: RProps

    class State(
            var artifacts: List<ArchetypeDetail>?,
            var projects: List<ProjectDetail>?,
            var runningProjects: List<String>?,

            var loading: Boolean = false,
            var errorMessage: String? = null,

            var creating: Boolean = false
    ): RState


    //-----------------------------------------------------------------------------------------------------------------
    override fun State.init(props: Props) {
        artifacts = null
        projects = null
        runningProjects = null
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun componentDidMount() {
        loadFromServerIfRequired()
        ErrorBus.subscribe(this)
    }


    override fun componentWillUnmount() {
        ErrorBus.unSubscribe(this)
    }


    override fun componentDidUpdate(prevProps: Props, prevState: State, snapshot: Any) {
        loadFromServerIfRequired()
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun onSuccess() {
        setState {
            errorMessage = null
        }
    }


    override fun onError(message: String) {
        setState {
            this.errorMessage = message
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
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
//    private fun onOpenNavigation() {
//        setState {
//            creating = false
//            errorMessage = null
//        }
//    }
//
//
//    private fun onCreateNavigation() {
//        setState {
//            creating = true
//            errorMessage = null
//        }
//    }


    private fun onCreateToggle() {
        setState {
            creating = ! state.creating
            errorMessage = null
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun RBuilder.render() {
        renderHeader()

        styledDiv {
            css {
                // offset position = fixed from AppBar above
                marginTop = 6.em

                // NB: without this the scroll bar ends without any space below project list
                paddingBottom = 0.1.em
            }

            renderBody()
        }
    }


    private fun RBuilder.renderBody() {
        renderErrorMessage()

        if (state.creating) {
            child(NewProjectScreen::class) {
                attrs {
                    artifacts = state.artifacts

                    didCreate = {
                        setState {
                            creating = false
                            projects = null
                        }
                        loadFromServerIfRequired()
                    }
                }
            }
        }
        else {
            child(ManageProjectsScreen::class) {
                attrs {
                    projects = state.projects
                    runningProjects = state.runningProjects

                    onProjectsChanged = {
                        setState {
                            projects = null
                        }
                        loadFromServerIfRequired()
                    }

                    onRunningChanged = {
                        setState {
                            runningProjects = null
                        }
                        loadFromServerIfRequired()
                    }
                }
            }
        }
    }


    private fun RBuilder.renderHeader() {
        child(MaterialAppBar::class) {
            attrs {
                position = "fixed"

                style = reactStyle {
                    backgroundColor = Color.white
                }
            }

            styledDiv {
                css {
                    width = 100.pct
                }

                styledDiv {
                    css {
                        float = Float.left

                        marginLeft = 1.em
                        marginTop = (0.5).em
                        marginRight = 1.em
                    }
                    renderLogo()
                }

                styledDiv {
                    css {
                        float = Float.left
                        marginTop = (-5).px
                    }

                    child(MaterialTabs::class) {
                        attrs {
                            textColor = "primary"
                            indicatorColor = "primary"

                            value = when {
                                state.creating -> 1
                                else -> 0
                            }

                            onChange = { _, index: Int ->
                                if (state.creating && index == 0 ||
                                        ! state.creating && index == 1) {
                                    onCreateToggle()
                                }
                            }
                        }

                        child(MaterialTab::class) {
                            attrs {
                                label = "Open"
                                icon = buildElement {
                                    child(LaunchIcon::class) {}
                                }
                            }
                        }

                        // TODO: https://github.com/mui-org/material-ui/issues/11653
                        child(MaterialTab::class) {
                            attrs {
                                label = "New Project"
                                icon = buildElement {
                                    child(AddCircleOutlineIcon::class) {}
                                }
                            }
                        }
                    }
                }

                styledDiv {
                    css {
                        float = Float.left

                        fontStyle = FontStyle.italic
                        fontSize = 1.5.em
                        marginTop = 1.em
                        marginLeft = 1.em
                        color = Color.black
                    }

//                    +"Kzen: Automate all the things"
                    +"Kzen: Automate your work"
                }
            }
        }
    }


    private fun RBuilder.renderLogo() {
        styledA {
            attrs {
                href = "/"
            }

            styledImg(src = "logo.png") {
                css {
                    height = 52.px
                }

                attrs {
                    title = "Kzen (home)"
                }
            }
        }
    }


    private fun RBuilder.renderErrorMessage() {
        if (state.errorMessage != null) {
            styledDiv {
                css {
                    color = Color.darkRed
                    fontWeight = FontWeight.bolder
                }

                +"Error: ${state.errorMessage}"
            }
        }
    }
}