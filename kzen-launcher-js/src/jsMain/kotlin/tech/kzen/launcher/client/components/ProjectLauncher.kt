package tech.kzen.launcher.client.components

import emotion.react.css
import mui.material.*
import mui.system.sx
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img
import tech.kzen.launcher.client.components.add.NewProjectScreen
import tech.kzen.launcher.client.components.manage.ManageProjectsScreen
import tech.kzen.launcher.client.service.ErrorBus
import tech.kzen.launcher.client.state.LauncherStore
import tech.kzen.launcher.client.wrap.*
import tech.kzen.launcher.common.api.staticResourcePath
import web.cssom.*
import web.dom.document
import web.html.HTMLMetaElement


//---------------------------------------------------------------------------------------------------------------------
external interface ProjectLauncherState: State {
    var errorMessage: String?
    var creating: Boolean
}


//---------------------------------------------------------------------------------------------------------------------
class ProjectLauncher(
    props: Props
):
    RComponent<Props, ProjectLauncherState>(props),
    ErrorBus.Subscriber,
    LauncherStore.Observer
{
    //-----------------------------------------------------------------------------------------------------------------
    override fun ProjectLauncherState.init(props: Props) {
        errorMessage = null
        creating = false
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun componentDidMount() {
        LauncherStore.subscribe(this)
        ErrorBus.subscribe(this)
        LauncherStore.loadIfRequired()
        LauncherStore.startPolling()
    }


    override fun componentWillUnmount() {
        LauncherStore.stopPolling()
        LauncherStore.unSubscribe(this)
        ErrorBus.unSubscribe(this)
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun onLauncherStoreChanged() {
        // Re-render from the store's latest snapshot.
        setState { }
    }


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
    private fun onCreateToggle() {
        setState {
            creating = !state.creating
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
                archetypes = LauncherStore.archetypes

                didCreate = {
                    setState {
                        creating = false
                    }
                    LauncherStore.invalidateProjects()
                }
            }
        }
        else {
            ManageProjectsScreen::class.react {
                projects = LauncherStore.projects
                runningProjects = LauncherStore.runningProjects
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
                        float = Float.left

                        marginLeft = 1.em
                        marginTop = (0.5).em
                        marginRight = 1.em
                    }
                    renderLogo()
                }

                div {
                    css {
                        float = Float.left
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
                                !state.creating && index == 1) {
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
                                AddCircleOutlinedIcon::class.react {}
                            }
                        }
                    }
                }

                div {
                    css {
                        float = Float.left

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


    // Logo hover text: "Kzen (home)" plus the running server's version + build timestamp (from the
    //  kzen-build meta tag emitted by indexPage), so the live build is always identifiable. Falls back
    //  to just "Kzen (home)" when the meta is absent/blank (e.g. a pure-IDE dev run without the stamp).
    private fun logoTitle(): String {
        val build = (document.querySelector("meta[name=\"kzen-build\"]") as? HTMLMetaElement)
            ?.content
            ?.takeIf { it.isNotBlank() }
        return if (build == null) "Kzen (home)" else "Kzen (home)\n$build"
    }


    private fun ChildrenBuilder.renderLogo() {
        a {
            href = "/"

            img {
                src = "$staticResourcePath/logo.png"

                css {
                    height = 52.px
                }

                title = logoTitle()
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
