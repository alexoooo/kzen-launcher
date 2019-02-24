package tech.kzen.launcher.client.components

import kotlinx.css.*
import kotlinx.html.title
import react.*
import styled.*
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.clientRestApi
import tech.kzen.launcher.client.api.shellRestApi
import tech.kzen.launcher.client.components.add.NewProjectScreen
import tech.kzen.launcher.client.components.manage.ManageProjectsScreen
import tech.kzen.launcher.client.service.ErrorBus
import tech.kzen.launcher.client.wrap.*
import tech.kzen.launcher.common.dto.ProjectDetail


class ProjectLauncher(
        props: ProjectLauncher.Props
):
        RComponent<ProjectLauncher.Props, ProjectLauncher.State>(props),
        ErrorBus.Subscriber
{
    //-----------------------------------------------------------------------------------------------------------------
    class Props(

    ): RProps

    class State(
            var artifacts: Map<String, String>?,
            var projects: List<ProjectDetail>?,
            var runningProjects: List<String>?,

            var loading: Boolean = false,
            var errorMessage: String? = null,

            var creating: Boolean = false
    ): RState


    //-----------------------------------------------------------------------------------------------------------------
    override fun ProjectLauncher.State.init(props: ProjectLauncher.Props) {
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
    private fun onCreateToggle() {
        setState {
            creating = ! state.creating
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun RBuilder.render() {
        renderHeader()

        renderErrorMessage()

        if (state.creating) {
            child(NewProjectScreen::class) {
                attrs {
                    artifacts = state.artifacts

                    didCreate = {
                        setState {
                            projects = null
                        }
                        loadFromServerIfRequired()
                    }
                }
            }
        }
        else {
//            console.log("^^^^ rendering ", state.projects, state.runningProjects)

            child(ManageProjectsScreen::class) {
                attrs {
                    projects = state.projects
                    runningProjects = state.runningProjects

                    didChange = {
                        loadFromServerIfRequired()
                    }
                }
            }
        }
    }


    private fun RBuilder.renderHeader() {
        child(MaterialAppBar::class) {
            attrs {
                position = "sticky"

                style = reactStyle {
                    backgroundColor = Color.white
                }
            }

            child(MaterialToolbar::class) {
//                attrs {
//                    style = reactStyle {
//                        width = 100.pct
//                    }
//                }

                child(MaterialTypography::class) {
                    attrs {
                        style = reactStyle {
                            width = 100.pct
                        }
                    }

                    styledSpan {
                        css {
                            float = Float.left
                        }

                        renderLogo()
                    }

                    styledSpan {
                        css {
                            marginLeft = 1.em
                        }

                        styledSpan {
                            css {
                                marginTop = 0.5.em
                                fontStyle = FontStyle.italic
                                fontSize = 1.5.em
                                display = Display.inlineBlock
                            }

                            +"Kzen: Automate all the things"
                        }
                    }
                }


                child(MaterialTypography::class) {
                    attrs {
                        style = reactStyle {
                            float = Float.right
                        }
                    }

                    child(MaterialButton::class) {
                        attrs {
                            variant = "outlined"

                            style = reactStyle {
                                width = 12.em
                                backgroundColor = Color("#649fff")

                                color =
                                        if (state.creating) {
                                            Color.white
                                        }
                                        else {
                                            Color.black
                                        }
                            }

                            onClick = ::onCreateToggle
                        }

                        child(AddCircleOutlineIcon::class) {
                            attrs {
                                style = reactStyle {
                                    marginRight = 0.25.em
                                }
                            }
                        }

                        +"New Project"
                    }
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
                    height = 35.px
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