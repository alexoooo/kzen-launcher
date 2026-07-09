package tech.kzen.launcher.client.components.manage

import react.ChildrenBuilder
import react.Props
import react.State
import tech.kzen.launcher.client.components.whiteCard
import tech.kzen.launcher.client.wrap.RComponent
import tech.kzen.launcher.client.wrap.react
import tech.kzen.launcher.common.dto.ProjectDetail


//-----------------------------------------------------------------------------------------------------------------
external interface ManageProjectsScreenProps: Props {
    var projects: List<ProjectDetail>?
    var runningProjects: List<String>?
}


//-----------------------------------------------------------------------------------------------------------------
class ManageProjectsScreen(
    props: ManageProjectsScreenProps
):
    RComponent<ManageProjectsScreenProps, State>(props)
{
    //-----------------------------------------------------------------------------------------------------------------
    override fun ChildrenBuilder.render() {
        whiteCard {
            renderRunning()
        }

        whiteCard {
            renderList()
        }
    }


    private fun ChildrenBuilder.renderRunning() {
        ProjectRunning::class.react {
            projects = props.runningProjects
        }
    }


    private fun ChildrenBuilder.renderList() {
        ProjectList::class.react {
            projects = props.projects
                ?.filter { !(props.runningProjects?.contains(it.name) ?: false) }
        }
    }
}
