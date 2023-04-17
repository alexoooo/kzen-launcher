package tech.kzen.launcher.client.components.manage

import csstype.Margin
import csstype.NamedColor
import csstype.PropertyName.Companion.margin
import csstype.em
import emotion.react.css
import mui.material.Card
import mui.material.CardContent
import react.*
import tech.kzen.launcher.client.wrap.*
import tech.kzen.launcher.common.dto.ProjectDetail


//-----------------------------------------------------------------------------------------------------------------
external interface ManageProjectsScreenProps: Props {
    var projects: List<ProjectDetail>?
    var runningProjects: List<String>?

    var onProjectsChanged: (() -> Unit)?
    var onRunningChanged: (() -> Unit)?
}


external interface ManageProjectsScreenState: State {
    var projects: List<ProjectDetail>?
    var runningProjects: List<String>?
}


//-----------------------------------------------------------------------------------------------------------------
class ManageProjectsScreen(
        props: ManageProjectsScreenProps
):
        RComponent<ManageProjectsScreenProps, ManageProjectsScreenState>(props)
{

    //-----------------------------------------------------------------------------------------------------------------
    override fun ManageProjectsScreenState.init(props: ManageProjectsScreenProps) {
//        console.log("ManageProjectsScreen State.init", props.projects, props.runningProjects, props)

        projects = props.projects
        runningProjects = props.runningProjects
    }


    override fun componentDidUpdate(
        prevProps: ManageProjectsScreenProps, prevState: ManageProjectsScreenState, snapshot: Any
    ) {
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
    override fun ChildrenBuilder.render() {
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


    private fun ChildrenBuilder.renderRunning() {
        Card {
            css {
                backgroundColor = NamedColor.white
                margin = Margin(2.em, 2.em, 2.em, 2.em)
            }

            CardContent {
                ProjectRunning::class.react {
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


    private fun ChildrenBuilder.renderList() {
        Card {
            css {
                backgroundColor = NamedColor.white
                margin = Margin(2.em, 2.em, 2.em, 2.em)
            }

            CardContent {
                ProjectList::class.react {
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