package tech.kzen.launcher.client.wrap.select


// Value/label carrier for the client's select fields (muiAutocompleteField).
// `value` is the stable identity used for selection + equality; `label` is the display text.
external interface SelectOption {
    var value: String
    var label: String
}
