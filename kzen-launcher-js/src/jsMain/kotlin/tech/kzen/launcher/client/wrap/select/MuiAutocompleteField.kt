package tech.kzen.launcher.client.wrap.select

import js.array.ReadonlyArray
import mui.material.Autocomplete
import mui.material.AutocompleteProps
import mui.material.Size
import mui.material.TextField
import react.ChildrenBuilder
import react.FC
import react.ReactNode
import react.create


// Spreads MUI's AutocompleteRenderInputParams onto the TextField props (slotProps / id / disabled /
// ...), which renderInput must forward for the combobox input to be wired up. Kept as a NON-inline
// top-level function so the js(...) Object.assign call is legal (js() is rejected inside inline lambdas).
private fun objectAssign(target: Any, source: Any) {
    js("Object.assign(target, source)")
}


// A labelled MUI Autocomplete select/filter field — the consistently-labelled select used across the
// client (the ergonomic replacement for a bare react-select). Options carry their identity + display
// text as a SelectOption (value/label); selection identity and equality are by `value`.
//
// `Autocomplete` ships as `FC<AutocompleteProps<*>>` (star-projected), so it is cast to the concrete
// FC<AutocompleteProps<SelectOption>> before invocation — the standard kotlin-wrappers idiom for generic
// components.
//
// Trimmed from kzen-auto's helper of the same name to the single call shape this client needs; add the
// multi-select / popover (forceOpen / onEscape / opaqueBackground / …) variants back from that sibling
// if a future field needs them.
fun ChildrenBuilder.muiAutocompleteField(
    label: String,
    options: Array<SelectOption>,
    selectedOption: SelectOption?,
    onSelect: (SelectOption) -> Unit,
    disableClearable: Boolean = false,
    disabled: Boolean = false,
    autoFocus: Boolean = false
) {
    val component = Autocomplete.unsafeCast<FC<AutocompleteProps<SelectOption>>>()
    component {
        this.options = options.unsafeCast<ReadonlyArray<SelectOption>>()
        this.value = selectedOption
        this.getOptionLabel = { it.label }
        this.isOptionEqualToValue = { a, b -> a.value == b.value }
        this.disableClearable = disableClearable
        // Auto-highlight the first (top) filtered option as the user types so Enter selects it without
        // first arrow-down'ing or hovering (MUI's own default is false — Enter does nothing until
        // something is highlighted).
        this.autoHighlight = true
        this.disabled = disabled
        this.fullWidth = true

        // onChange is (event, value: Any, reason, details) — value is the picked option (non-null here
        // since the field is never cleared while editing); narrow it back to SelectOption.
        this.onChange = { _, picked, _, _ ->
            onSelect(picked.unsafeCast<SelectOption>())
        }

        this.renderInput = { params ->
            TextField.create {
                objectAssign(this, params)
                this.label = ReactNode(label)
                this.size = Size.small
                if (autoFocus) {
                    this.autoFocus = true
                }
            }
        }
    }
}
