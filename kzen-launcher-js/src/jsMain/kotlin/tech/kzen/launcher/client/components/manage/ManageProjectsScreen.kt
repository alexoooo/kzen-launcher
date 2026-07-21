package tech.kzen.launcher.client.components.manage

import react.ChildrenBuilder
import react.Props
import react.State
import tech.kzen.launcher.client.components.whiteCard
import tech.kzen.launcher.client.wrap.RComponent
import tech.kzen.launcher.client.wrap.react
import tech.kzen.launcher.common.dto.ProjectDetail
import tech.kzen.launcher.common.dto.RunningProject


//-----------------------------------------------------------------------------------------------------------------
external interface ManageProjectsScreenProps: Props {
    var projects: List<ProjectDetail>?
    var runningProjects: List<RunningProject>?
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
            restartableNames = props.projects?.map { it.name }?.toSet()
        }
    }


    private fun ChildrenBuilder.renderList() {
        val activeNames = props.runningProjects?.map { it.name }?.toSet() ?: emptySet()
        ProjectList::class.react {
            // Hide any project that currently has an active job (starting/running/stopping/failed) — it
            //  is shown in the Running section instead.
            projects = props.projects
                ?.filter { it.name !in activeNames }
        }
    }
}
