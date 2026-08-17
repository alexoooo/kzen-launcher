package tech.kzen.launcher.client.components.manage

import emotion.react.css
import js.objects.unsafeJso
import mui.material.Button
import mui.material.ButtonColor
import mui.material.ButtonVariant
import mui.material.Dialog
import mui.material.DialogActions
import mui.material.DialogContent
import mui.material.DialogContentText
import mui.material.DialogTitle
import mui.system.sx
import react.*
import react.dom.html.ReactHTML.div
import tech.kzen.launcher.client.components.buttonIcon
import tech.kzen.launcher.client.components.buttonSpinner
import tech.kzen.launcher.client.wrap.*
import tech.kzen.launcher.client.wrap.select.SelectOption
import tech.kzen.launcher.client.wrap.select.muiAutocompleteField
import tech.kzen.launcher.common.dto.ArchetypeDetail
import tech.kzen.launcher.common.dto.ProjectDetail
import tech.kzen.launcher.common.util.VersionNumbers
import web.cssom.*
import kotlin.js.Promise


//---------------------------------------------------------------------------------------------------------------------
external interface ProjectUpgradeControlProps: Props {
    var project: ProjectDetail
    var archetypes: List<ArchetypeDetail>?

    var onUpgrade: ((ProjectDetail, String) -> Promise<Unit>)
}


external interface ProjectUpgradeControlState: State {
    var upgrading: Boolean
    var confirming: Boolean
    var selectedType: String?
}


//---------------------------------------------------------------------------------------------------------------------
// Replaces a project's program with another cached archetype version: the offer gate, the version dialog,
//  and the in-flight button state. Renders nothing when no applicable version is cached.
class ProjectUpgradeControl(
    props: ProjectUpgradeControlProps
): RComponent<ProjectUpgradeControlProps, ProjectUpgradeControlState>(props) {
    //-----------------------------------------------------------------------------------------------------------------
    companion object {
        // Archetype entries the project could upgrade to: those sharing its recorded base name. A project with
        //  an unknown archetype (imported / pre-SH4) matches every entry, so one upgrade lets it adopt tracking.
        private fun candidatesFor(
            project: ProjectDetail,
            archetypes: List<ArchetypeDetail>
        ): List<ArchetypeDetail> {
            if (project.archetype == ProjectDetail.unknownValue) {
                return archetypes
            }
            return archetypes.filter { it.archetype == project.archetype }
        }


        // A snapshot recorded version can be re-installed from a same-version cached zip (a rebuilt dev
        //  snapshot is re-acquired under the same version string each boot) — without this the upgrade path
        //  itself would be untestable in the dev loop.
        private fun isReinstall(recorded: String, candidates: List<ArchetypeDetail>): Boolean {
            return VersionNumbers.isSnapshot(recorded) && candidates.any { it.version == recorded }
        }


        // Offer the Upgrade action when the project's archetype is unknown (and anything is cached), or a
        //  strictly-newer version is cached, or the dev-reinstall clause applies.
        private fun shouldOffer(project: ProjectDetail, candidates: List<ArchetypeDetail>): Boolean {
            if (candidates.isEmpty()) {
                return false
            }
            val recorded = project.version
            return recorded == ProjectDetail.unknownValue ||
                candidates.any { VersionNumbers.compare(it.version, recorded) > 0 } ||
                isReinstall(recorded, candidates)
        }


        // Dialog list: every candidate except the recorded version (kept version-descending, as the server
        //  sorts them), plus the equal snapshot in the reinstall case.
        private fun dialogCandidates(project: ProjectDetail, candidates: List<ArchetypeDetail>): List<ArchetypeDetail> {
            val recorded = project.version
            val reinstall = isReinstall(recorded, candidates)
            return candidates.filter { it.version != recorded || reinstall }
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun ProjectUpgradeControlState.init(props: ProjectUpgradeControlProps) {
        upgrading = false
        confirming = false
        selectedType = null
    }


    //-----------------------------------------------------------------------------------------------------------------
    // Open the version dialog, defaulting the selection to the newest offered candidate (the server sorts
    //  them version-descending, so the first is newest).
    private fun onRequest(defaultType: String?) {
        if (state.upgrading) {
            return
        }

        setState {
            confirming = true
            selectedType = defaultType
        }
    }


    private fun onCancel() {
        setState {
            confirming = false
        }
    }


    private fun onSelect(type: String) {
        setState {
            selectedType = type
        }
    }


    // Fire the upgrade and show a spinner on the button until it settles. On success the store refreshes the
    //  list and the row shows the new recorded version; on failure (e.g. a 409 while running) it stays, with
    //  the error surfaced by the interceptor, and the spinner clears so the user can retry.
    private fun onConfirm() {
        val type = state.selectedType
            ?: return

        setState {
            confirming = false
            upgrading = true
        }

        props.onUpgrade(props.project, type)
            .then { clearUpgrading() }
            .catch { clearUpgrading() }
    }


    private fun clearUpgrading() {
        setState {
            upgrading = false
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun ChildrenBuilder.render() {
        val archetypes = props.archetypes
            ?: return

        val candidates = candidatesFor(props.project, archetypes)
        if (!shouldOffer(props.project, candidates)) {
            return
        }

        val dialogCandidates = dialogCandidates(props.project, candidates)
        if (dialogCandidates.isEmpty()) {
            return
        }

        div {
            css {
                display = Display.inlineBlock
                marginLeft = 1.em
            }

            Button {
                variant = ButtonVariant.outlined
                disabled = state.upgrading
                onClick = { onRequest(dialogCandidates.first().name) }

                if (state.upgrading) {
                    buttonSpinner()
                }
                else {
                    buttonIcon(UpgradeIcon::class)
                }

                +"Upgrade"
            }

            renderDialog(dialogCandidates)
        }
    }


    private fun ChildrenBuilder.renderDialog(dialogCandidates: List<ArchetypeDetail>) {
        val recorded = props.project.version
        val selected = dialogCandidates.find { it.name == state.selectedType }

        // Warn on a non-strictly-newer target: a downgrade, a same-version reinstall, or an unknown recorded
        //  version (where "newer" can't be established) — data written by a newer version may not load back.
        val notNewer = recorded == ProjectDetail.unknownValue ||
            selected == null ||
            VersionNumbers.compare(selected.version, recorded) <= 0

        Dialog {
            open = state.confirming
            onClose = { _, _ -> onCancel() }

            DialogTitle {
                +"Upgrade project"
            }

            DialogContent {
                DialogContentText {
                    +("Replace the program of \"${props.project.name}\" with a selected version. Your " +
                        "project data (notation, work, logs) is preserved; only the jar is replaced.")
                }

                div {
                    css {
                        marginTop = 1.em
                        marginBottom = 1.em
                        width = 40.em
                        maxWidth = 100.pct
                    }

                    val options = dialogCandidates
                        .map {
                            val option: SelectOption = unsafeJso {
                                value = it.name
                                label = "v${it.version}"
                            }
                            option
                        }
                        .toTypedArray()

                    muiAutocompleteField(
                        label = "Version",
                        options = options,
                        selectedOption = options.find { it.value == state.selectedType },
                        onSelect = { onSelect(it.value) },
                        disableClearable = true,
                        disabled = state.upgrading)
                }

                if (notNewer) {
                    DialogContentText {
                        sx {
                            color = NamedColor.darkred
                        }
                        +("Downgrade / reinstall — project data created by a newer version may not load; " +
                            "a backup of your project folder is recommended.")
                    }
                }
            }

            DialogActions {
                Button {
                    variant = ButtonVariant.outlined
                    onClick = { onCancel() }

                    +"Cancel"
                }

                Button {
                    variant = ButtonVariant.contained
                    color = if (notNewer) { ButtonColor.warning } else { ButtonColor.primary }
                    disabled = state.selectedType == null
                    onClick = { onConfirm() }

                    buttonIcon(UpgradeIcon::class)

                    +"Upgrade"
                }
            }
        }
    }
}
