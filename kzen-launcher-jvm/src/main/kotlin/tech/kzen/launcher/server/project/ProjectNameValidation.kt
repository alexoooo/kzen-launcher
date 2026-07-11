package tech.kzen.launcher.server.project


// A project name becomes a directory name under the project home (ProjectCreator.create /
//  ProjectRepo.rename resolve it directly) and a routing prefix in kzen-shell's proxy URL space
//  (/<name>/...). Server-side validation keeps a crafted name from escaping the project home
//  (../x), colliding with the routing contract (main, shell), or being unrepresentable on
//  Windows. The client UI has its own softer checks; this is the authoritative gate.
object ProjectNameValidation {
    //-----------------------------------------------------------------------------------------------------------------
    private const val maxLength = 128

    // Names the shell's proxy routing reserves: 'main' aliases the launcher, /shell/* are the
    //  shell's own endpoints.
    private val reservedRoutingNames = setOf("main", "shell")

    // Windows device names are reserved even with an extension (CON.txt).
    private val windowsDeviceNames: Set<String> =
        setOf("con", "prn", "aux", "nul") +
        (1..9).map { "com$it" } +
        (1..9).map { "lpt$it" }

    // Characters invalid in Windows file names (plus path separators on all platforms).
    private val invalidCharacters = setOf('<', '>', ':', '"', '/', '\\', '|', '?', '*')


    //-----------------------------------------------------------------------------------------------------------------
    fun errorOrNull(name: String): String? {
        if (name.isBlank()) {
            return "project name required"
        }
        if (name.length > maxLength) {
            return "project name too long (max $maxLength): ${name.length}"
        }
        if (name == "." || name == "..") {
            return "invalid project name: $name"
        }
        for (char in name) {
            if (char < ' ' || char in invalidCharacters) {
                return "invalid character in project name: $name"
            }
        }
        if (name.endsWith(".") || name.endsWith(" ")) {
            return "project name must not end with a dot or space: $name"
        }

        val lower = name.lowercase()
        if (lower in reservedRoutingNames) {
            return "reserved project name: $name"
        }
        if (lower.substringBefore('.') in windowsDeviceNames) {
            return "reserved device name: $name"
        }

        return null
    }


    fun check(name: String) {
        val error = errorOrNull(name)
        require(error == null) { error!! }
    }
}
