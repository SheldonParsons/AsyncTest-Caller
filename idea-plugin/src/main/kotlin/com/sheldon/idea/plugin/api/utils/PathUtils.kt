package com.sheldon.idea.plugin.api.utils
object PathUtils {
    fun normalizeToAsyncTestPath(springPath: String?): String {
        if (springPath == null) return ""
        val result = StringBuilder()
        val length = springPath.length
        var i = 0
        while (i < length) {
            val c = springPath[i]
            if (c == '{') {
                val variableContent = extractBalancedBraceContent(springPath, i)
                if (variableContent != null) {
                    val varName = getVariableName(variableContent)
                    result.append("{{").append(varName).append("}}")
                    i += variableContent.length + 2
                    continue
                }
            }
            result.append(c)
            i++
        }
        return result.toString()
    }
    private fun extractBalancedBraceContent(text: String, startIndex: Int): String? {
        var balance = 0
        for (j in startIndex until text.length) {
            val char = text[j]
            if (char == '{') {
                balance++
            } else if (char == '}') {
                balance--
            }
            if (balance == 0) {
                return text.substring(startIndex + 1, j)
            }
        }
        return null
    }
    private fun getVariableName(content: String): String {
        val colonIndex = content.indexOf(':')
        return if (colonIndex != -1) {
            content.substring(0, colonIndex).trim()
        } else {
            content.trim()
        }
    }
}