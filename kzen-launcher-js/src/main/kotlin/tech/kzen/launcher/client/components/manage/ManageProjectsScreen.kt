package tech.kzen.launcher.client.components.manage

import kotlinx.css.*
import react.*
import styled.css
import styled.styledDiv
import tech.kzen.launcher.client.wrap.MaterialCard
import tech.kzen.launcher.client.wrap.MaterialCardContent
import tech.kzen.launcher.client.wrap.reactStyle
import tech.kzen.launcher.common.dto.ProjectDetail


class ManageProjectsScreen(
        props: Props
):
        RComponent<ManageProjectsScreen.Props, ManageProjectsScreen.State>(props)
{
    //-----------------------------------------------------------------------------------------------------------------
    class Props(
            var projects: List<ProjectDetail>?,
            var runningProjects: List<String>?,

            var onProjectsChanged: (() -> Unit)?,
            var onRunningChanged: (() -> Unit)?
    ): RProps


    class State(
            var projects: List<ProjectDetail>?,
            var runningProjects: List<String>?
    ): RState


    //-----------------------------------------------------------------------------------------------------------------
    override fun State.init(props: Props) {
//        console.log("ManageProjectsScreen State.init", props.projects, props.runningProjects, props)

        projects = props.projects
        runningProjects = props.runningProjects
    }


    override fun componentDidUpdate(prevProps: Props, prevState: State, snapshot: Any) {
//        console.log("ManageProjectsScreen componentDidUpdate", props, prevProps)

        if (props.projects != prevProps.projects) {
            setState {
                projects = props.projects
            }
        }

        if (props.runningProjects != prevProps.runningProjects) {
            setState {
                runningProjects = props.runningProjects
            }
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun RBuilder.render() {
//        console.log("ManageProjectsScreen - render", state, props)

//        styledDiv {
            renderRunning()
//        }

//        styledDiv {
//            css {
//                marginBottom = 2.em
//            }
            renderList()
//        }
    }


    private fun RBuilder.renderRunning() {
        child(MaterialCard::class) {
            attrs {
                style = reactStyle {
                    backgroundColor = Color.white
                    margin(2.em)
                }
            }

            child(MaterialCardContent::class) {
                child(ProjectRunning::class) {
                    attrs {
                        projects = state.runningProjects

                        didStop = {
                            setState {
                                runningProjects = null
                            }

                            props.onRunningChanged?.invoke()
                        }
                    }
                }
            }
        }
    }


    private fun RBuilder.renderList() {
        child(MaterialCard::class) {
            attrs {
                style = reactStyle {
                    backgroundColor = Color.white
                    margin(2.em)
                }
            }

            child(MaterialCardContent::class) {
                child(ProjectList::class) {
                    attrs {
                        projects = state.projects
                                ?.filter{ ! (state.runningProjects?.contains(it.name) ?: false) }

                        didStart = {
                            props.onRunningChanged?.invoke()
                        }

                        didRemove = {
                            props.onProjectsChanged?.invoke()
                        }

                        didDelete = {
                            props.onProjectsChanged?.invoke()
                        }

                        didRename = {
                            props.onProjectsChanged?.invoke()
                        }

                        didChangeJvmArgs = {
                            props.onProjectsChanged?.invoke()
                        }
                    }
                }
            }
        }
    }
}