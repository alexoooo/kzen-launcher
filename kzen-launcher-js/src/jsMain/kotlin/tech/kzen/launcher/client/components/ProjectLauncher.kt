package tech.kzen.launcher.client.components

import emotion.react.css
import mui.material.*
import mui.system.sx
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.clientRestApi
import tech.kzen.launcher.client.api.shellRestApi
import tech.kzen.launcher.client.components.add.NewProjectScreen
import tech.kzen.launcher.client.components.manage.ManageProjectsScreen
import tech.kzen.launcher.client.service.ErrorBus
import tech.kzen.launcher.client.wrap.AddCircleOutlineIcon
import tech.kzen.launcher.client.wrap.LaunchIcon
import tech.kzen.launcher.client.wrap.RComponent
import tech.kzen.launcher.client.wrap.setState
import tech.kzen.launcher.common.api.staticResourcePath
import tech.kzen.launcher.common.dto.ArchetypeDetail
import tech.kzen.launcher.common.dto.ProjectDetail
import web.cssom.*


//---------------------------------------------------------------------------------------------------------------------
external interface ProjectLauncherState: State {
    var archetypes: List<ArchetypeDetail>?
    var projects: List<ProjectDetail>?
    var runningProjects: List<String>?

    var loading: Boolean
    var errorMessage: String?

    var creating: Boolean
}


//---------------------------------------------------------------------------------------------------------------------
class ProjectLauncher(
    props: Props
):
    RComponent<Props, ProjectLauncherState>(props),
    ErrorBus.Subscriber
{
    //-----------------------------------------------------------------------------------------------------------------
    override fun ProjectLauncherState.init(props: Props) {
        archetypes = null
        projects = null
        runningProjects = null
        loading = false
        errorMessage = null
        creating = false
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun componentDidMount() {
        loadFromServerIfRequired()
        ErrorBus.subscribe(this)
    }


    override fun componentWillUnmount() {
        ErrorBus.unSubscribe(this)
    }


    override fun componentDidUpdate(prevProps: Props, prevState: ProjectLauncherState, snapshot: Any) {
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
        val needArchetypes = (state.archetypes == null)
        val needProjects = (state.projects == null)
        val needRunningProjects = (state.runningProjects == null)

        if (! (needArchetypes || needProjects || needRunningProjects)) {
            return
        }

        setState {
            loading = true
        }

        if (needArchetypes) {
            val response = clientRestApi.listArchetypes()
//            console.log("$$ artifacts: $response")

            setState {
                archetypes = response
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
    private fun onCreateToggle() {
        setState {
            creating = ! state.creating
            errorMessage = null
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun ChildrenBuilder.render() {
        renderHeader()

        div {
            css {
                // offset position = fixed from AppBar above
                marginTop = 6.em

                // NB: without this the scroll bar ends without any space below project list
                paddingBottom = 0.1.em
            }

            renderBody()
        }
    }


    private fun ChildrenBuilder.renderBody() {
        renderErrorMessage()

        if (state.creating) {
            NewProjectScreen::class.react {
                archetypes = state.archetypes

                didCreate = {
                    setState {
                        creating = false
                        projects = null
                    }
                    loadFromServerIfRequired()
                }
            }
        }
        else {
            ManageProjectsScreen::class.react {
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


    private fun ChildrenBuilder.renderHeader() {
        AppBar {
            position = AppBarPosition.fixed

            sx {
                backgroundColor = NamedColor.white
            }

            div {
                css {
                    width = 100.pct
                }

                div {
                    css {
                        float = web.cssom.Float.left

                        marginLeft = 1.em
                        marginTop = (0.5).em
                        marginRight = 1.em
                    }
                    renderLogo()
                }

                div {
                    css {
                        float = web.cssom.Float.left
                        marginTop = (-5).px
                    }

                    Tabs {
                        textColor = TabsTextColor.primary
                        indicatorColor = TabsIndicatorColor.primary

                        value = when {
                            state.creating -> 1
                            else -> 0
                        }

//                        onChange = { _, index: Int ->
                        asDynamic().onChange = { _: Any, index: Int ->
                            if (state.creating && index == 0 ||
                                ! state.creating && index == 1) {
                                onCreateToggle()
                            }
                        }

                        Tab {
                            label = ReactNode("Open")
                            icon = Fragment.create {
                                LaunchIcon::class.react {}
                            }
                        }

                        // TODO: https://github.com/mui-org/material-ui/issues/11653
                        Tab {
                            label = ReactNode("New Project")
                            icon = Fragment.create {
                                AddCircleOutlineIcon::class.react {}
                            }
                        }
                    }
                }

                div {
                    css {
                        float = web.cssom.Float.left

                        fontStyle = FontStyle.italic
                        fontSize = 1.5.em
                        marginTop = 1.em
                        marginLeft = 1.em
                        color = NamedColor.black
                    }

                    +"Kzen: Automation and reports"
                }
            }
        }
    }


    private fun ChildrenBuilder.renderLogo() {
        a {
            href = "/"

            img {
                src = "$staticResourcePath/logo.png"

                css {
                    height = 52.px
                }

                title = "Kzen (home)"
            }
        }
    }


    private fun ChildrenBuilder.renderErrorMessage() {
        if (state.errorMessage != null) {
            div {
                css {
                    color = NamedColor.darkred
                    fontWeight = FontWeight.bolder
                }

                +"Error: ${state.errorMessage}"
            }
        }
    }
}