package tech.kzen.launcher.common.util


object IoUtil {

    // from kzen-lib YamlUtils

    fun escapeJson(
            value: Any
    ): String {
        if (value is Boolean) {
            return escapeJsonBoolean(value)
        }

        if (value is String) {
            return escapeJsonString(value)
        }

        throw UnsupportedOperationException("Not implemented (yed): $value")
    }


    fun escapeJsonBoolean(
            value: Boolean
    ): String {
        return if (value) "true" else "false"
    }


    fun escapeJsonString(
            unescaped: String
    ): String {
        val output = StringBuilder()

        for (i in 0 until unescaped.length) {
            val ch = unescaped[i]

            val escaped: Any =
                    when (ch) {
                        0.toChar() ->
                            throw IllegalArgumentException("Zero char not allowed")

                        '\r' -> "\\r"
                        '\n' -> "\\n"
                        '\t' -> "\\t"
                        '\\' -> "\\\\"
                        '\b' -> "\\b"

                        '\u000C' -> "\\f"


                        '"' -> "\\\""
                        '\'' -> '\''


                        in 128.toChar() .. '\uffff' -> {
                            val hex = ch.toInt().toString(16)
                            val prefixed = "000$hex"
                            val padded = prefixed.substring(prefixed.length - 4)
                            "\\u$padded"
                        }


                        else ->
                            ch
                    }

            output.append(escaped)
        }

        return "\"$output\""
    }
}