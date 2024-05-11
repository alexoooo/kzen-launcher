package tech.kzen.launcher.client.components.add

import csstype.*
import emotion.react.css
import js.objects.jso
import mui.material.*
import mui.system.sx
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.onChange
import tech.kzen.launcher.client.api.async
import tech.kzen.launcher.client.api.clientRestApi
import tech.kzen.launcher.client.wrap.*
import tech.kzen.launcher.common.dto.ArchetypeDetail
import web.cssom.*
import web.html.HTMLInputElement
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
}


//---------------------------------------------------------------------------------------------------------------------
class NewProjectScreen(
    props: NewProjectScreenProps
): RComponent<NewProjectScreenProps, NewProjectScreenState>(props) {
    //-----------------------------------------------------------------------------------------------------------------
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
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun NewProjectScreenState.init(props: NewProjectScreenProps) {
        console.log("init: props.archetypes - ${props.archetypes}")

        name = newInitialName()
        path = defaultImportPath
        type = props.archetypes?.iterator()?.let {
            if (it.hasNext())
                it.next().name
            else null
        }
    }


    override fun componentDidUpdate(
        prevProps: NewProjectScreenProps,
        prevState: NewProjectScreenState,
        snapshot: Any
    ) {
        console.log("update: ${state.type} - ${props.archetypes}")

        if (state.type == null && props.archetypes != null) {
            setState {
                type = props.archetypes!!.iterator().next().name
            }
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    private fun onNameChange(projectName: String) {
//        console.log("%%%%5 onNameChange", projectName)

        setState {
            name = projectName
        }
    }


    private fun onTypeChange(projectType: String) {
//        console.log("%%%%5 onTypeChange", projectType)

        setState {
            type = projectType
        }
    }


    private fun onCreate() {
//        console.log("onSubmit: props - ${state.name} | ${state.type}")

        async {
            check(state.type != null) {"Type missing"}
            clientRestApi.createProject(state.name, state.type!!)

            setState {
                name = newInitialName()
                type = null
            }

            props.didCreate?.invoke()
        }
    }


    private fun onPathChange(projectPath: String) {
        setState {
            path = projectPath
        }
    }


    private fun onImport() {
        async {
            check(state.path.isNotBlank()) {"Path missing"}
            clientRestApi.importProject(state.path)

            setState {
                path = defaultImportPath
            }

            props.didCreate?.invoke()
        }
    }


    //-----------------------------------------------------------------------------------------------------------------
    override fun ChildrenBuilder.render() {
//        console.log("render: ${props.artifacts} | ${state.name} | ${state.type}")

        Paper {
            sx {
                backgroundColor = NamedColor.white
                margin = Margin(2.em, 2.em, 2.em, 2.em)
            }

            CardContent {
                h2 {
                    css {
                        marginTop = 0.px
                    }
                    +"Create New"
                }

                renderCreate()
            }
        }

        Card {
            sx {
                backgroundColor = NamedColor.white
                margin = Margin(2.em, 2.em, 2.em, 2.em)
            }

            CardContent {
                h2 {
                    css {
                        marginTop = 0.px
                    }
                    +"Import Existing"
                }

                renderImport()
            }
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
                    onClick = { onCreate() }

                    CreateIcon::class.react {
                        style = jso {
                            marginRight = 0.25.em
                        }
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
                    onClick = { onImport() }

                    RedoIcon::class.react {
                        style = jso {
                            marginRight = 0.25.em
                        }
                    }

                    +"Import"
                }
            }
        }
    }


    private fun ChildrenBuilder.renderName() {
        TextField {
            sx {
                width = 36.em
            }

            label = ReactNode("Name")
            value = state.name

            onChange = {
                val target = it.target as HTMLInputElement
                onNameChange(target.value)
            }
        }

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
        if (props.archetypes == null || state.type == null) {
            +"Loading... ${props.archetypes} - ${state.type}"
        }
        else {
            div {
                css {
                    width = 36.em
                    position = Position.relative
                }
                val selectOptions = props
                        .archetypes!!
                        .map {
                            val option: ReactSelectOption = jso {
                                value = it.name
                                label = it.title + " - " + it.description
                            }
                            option
                        }
                        .toTypedArray()

                val selectId = "material-react-select-id"

                InputLabel {
                    htmlFor = selectId

                    sx {
                        fontSize = 0.8.em
                    }

                    +"Type"
                }

                ReactSelect::class.react {
                    id = selectId
                    value = selectOptions.find { it.value == state.type }

                    options = selectOptions

                    onChange = {
                        onTypeChange(it.value)
                    }
                }
            }
        }
    }


    private fun ChildrenBuilder.renderPath() {
        TextField {
            sx {
                width = 36.em
            }

            label = ReactNode("Path")
            value = state.path

            onChange = {
                val target = it.target as HTMLInputElement
                onPathChange(target.value)
            }
        }
    }
}
