package tech.kzen.launcher.client.components.add

import emotion.react.css
import js.objects.unsafeJso
import mui.material.*
import mui.system.sx
import react.ChildrenBuilder
import react.Props
import react.State
import react.dom.html.ReactHTML.div
import tech.kzen.launcher.client.api.clientRestApi
import tech.kzen.launcher.client.api.launchUiAction
import tech.kzen.launcher.client.components.buttonIcon
import tech.kzen.launcher.client.components.sectionHeading
import tech.kzen.launcher.client.components.whiteCard
import tech.kzen.launcher.client.components.wideTextField
import tech.kzen.launcher.client.wrap.*
import tech.kzen.launcher.client.wrap.select.SelectOption
import tech.kzen.launcher.client.wrap.select.muiAutocompleteField
import tech.kzen.launcher.common.dto.ArchetypeDetail
import tech.kzen.launcher.common.util.VersionNumbers
import web.cssom.*
import kotlin.js.Date


//---------------------------------------------------------------------------------------------------------------------
external interface NewProjectScreenProps: Props {
    var archetypes: List<ArchetypeDetail>?
    var didCreate: (() -> Unit)?
}


external interface NewProjectScreenState: State {
    var name: String
    var type: String?
    var path: String

    var creating: Boolean
    var importing: Boolean
}


//---------------------------------------------------------------------------------------------------------------------
class NewProjectScreen(
    props: NewProjectScreenProps
): RComponent<NewProjectScreenProps, NewProjectScreenState>(props) {
    //-----------------------------------------------------------------------------------------------------------------
    @Suppress("ConstPropertyName")
    companion object {
        private const val defaultNamePrefix = "My New Project"
        private const val defaultImportPath = "../kzen-proj/existing-project-name"

        private fun newInitialName(): String {
            val date = Date()

            val timestampSuffix =
                    date.getFullYear().toString() + "-" +
                    ("0" + (date.getMonth() + 1)).takeLast(2) + "-" +
                    ("0" + date.getDate()).takeLast(2) + " " +
                    ("0" + date.getHours()).takeLast(2) + "-" +
                    ("0" + date.getMinutes()).takeLast(2) + "-" +
                    ("0" + date.getSeconds()).takeLast(2)

            return "$defaultNamePrefix - $timestampSuffix"
        }


        // New Project offers only the LATEST version per archetype name (the full per-version list is for the
        //  Upgrade action, not fresh creation). The server sorts entries version-descending, so the first
        //  parseable entry of each base name is its latest. Unparseable-version entries are always shown
        //  individually — never hide a cached artifact (the kzen-project-custom.zip case).
        fun latestPerName(archetypes: List<ArchetypeDetail>): List<ArchetypeDetail> {
            val result = mutableListOf<ArchetypeDetail>()
            val seenBase = mutableSetOf<String>()
            for (archetype in archetypes) {
                if (!VersionNumbers.parses(archetype.version)) {
                    result.add(archetype)
                    continue
                }
                if (archetype.archetype in seenBase) {
                    continue
                }
                seenBase.add(archetype.archetype)
                result.add(archetype)
            }
            return result
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun NewProjectScreenState.init(props: NewProjectScreenProps) {
        name = newInitialName()
        path = defaultImportPath
        type = props.archetypes?.firstOrNull()?.name
        creating = false
        importing = false
    }


    override fun componentDidUpdate(
        prevProps: NewProjectScreenProps,
        prevState: NewProjectScreenState,
        snapshot: Any
    ) {
        // firstOrNull: an offline boot can serve an empty catalogue, which must not crash here
        val defaultType = props.archetypes?.firstOrNull()?.name
        if (state.type == null && defaultType != null) {
            setState {
                type = defaultType
            }
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    private fun onNameChange(projectName: String) {
        setState {
            name = projectName
        }
    }


    private fun onTypeChange(projectType: String) {
        setState {
            type = projectType
        }
    }


    private fun onCreate() {
        setState {
            creating = true
        }

        launchUiAction {
            try {
                clientRestApi.createProject(state.name, state.type!!)

                setState {
                    name = newInitialName()
                    type = null
                }

                props.didCreate?.invoke()
            }
            finally {
                setState {
                    creating = false
                }
            }
        }
    }


    private fun onPathChange(projectPath: String) {
        setState {
            path = projectPath
        }
    }


    private fun onImport() {
        setState {
            importing = true
        }

        launchUiAction {
            try {
                clientRestApi.importProject(state.path)

                setState {
                    path = defaultImportPath
                }

                props.didCreate?.invoke()
            }
            finally {
                setState {
                    importing = false
                }
            }
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun ChildrenBuilder.render() {
        whiteCard {
            sectionHeading("Create New")
            renderCreate()
        }

        whiteCard {
            sectionHeading("Import Existing")
            renderImport()
        }
    }


    private fun ChildrenBuilder.renderCreate() {
        div {
            css {
                display = Display.inlineBlock
            }

            div {
                renderName()
            }

            div {
                css {
                    marginTop = 1.em
                    marginBottom = 1.em
                }

                renderTypeSelect()
            }

            div {
                Button {
                    variant = ButtonVariant.outlined
                    disabled = state.creating || state.type == null
                    onClick = { onCreate() }

                    if (state.creating) {
                        CircularProgress {
                            sx {
                                width = 1.em
                                height = 1.em
                                marginRight = 0.25.em
                            }
                        }
                    }
                    else {
                        buttonIcon(CreateIcon::class)
                    }

                    +"Create"
                }
            }
        }
    }


    private fun ChildrenBuilder.renderImport() {
        div {
            css {
                display = Display.inlineBlock
            }

            div {
                css {
                    marginBottom = 1.em
                }

                renderPath()
            }

            div {
                Button {
                    variant = ButtonVariant.outlined
                    disabled = state.importing || state.path.isBlank()
                    onClick = { onImport() }

                    if (state.importing) {
                        CircularProgress {
                            sx {
                                width = 1.em
                                height = 1.em
                                marginRight = 0.25.em
                            }
                        }
                    }
                    else {
                        buttonIcon(RedoIcon::class)
                    }

                    +"Import"
                }
            }
        }
    }


    private fun ChildrenBuilder.renderName() {
        wideTextField("Name", state.name, ::onNameChange)

        div {
            title = "Must be a valid file name"

            css {
                display = Display.inlineBlock
                marginTop = 1.em
                marginLeft = 0.5.em
            }

            InfoIcon::class.react {}
        }
    }


    private fun ChildrenBuilder.renderTypeSelect() {
        val archetypes = props.archetypes
        if (archetypes == null || state.type == null) {
            +"Loading..."
        }
        else {
            div {
                css {
                    // Wide enough to show the longest catalogue entry without truncation, e.g.
                    //  "Automation and Reporting - Visually control a browser and more - v0.30.0-SNAPSHOT"
                    width = 46.em
                }

                val selectOptions = latestPerName(archetypes)
                        .map {
                            val option: SelectOption = unsafeJso {
                                value = it.name
                                label = it.title + " - " + it.description
                            }
                            option
                        }
                        .toTypedArray()

                muiAutocompleteField(
                        label = "Type",
                        options = selectOptions,
                        selectedOption = selectOptions.find { it.value == state.type },
                        onSelect = { onTypeChange(it.value) },
                        disableClearable = true,
                        disabled = state.creating)
            }
        }
    }


    private fun ChildrenBuilder.renderPath() {
        wideTextField("Path", state.path, ::onPathChange)
    }
}
