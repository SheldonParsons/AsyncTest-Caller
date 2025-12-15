package com.sheldon.idea.plugin.api.utils

object PathUtils {

    /**
     * 将 Spring Path 转换为 AsyncTest Path
     * 支持处理嵌套花括号的情况，例如 {id:[0-9]{4}} -> {{id}}
     */
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
                    i += variableContent.length + 2 // +2 是因为要把外层的 {} 加上
                    continue
                }
            }
            result.append(c)
            i++
        }

        return result.toString()
    }

    /**
     * 寻找与当前起始位置 '{' 配对的 '}'
     * 返回花括号内部的字符串（不含外层 {}）
     */
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

    /**
     * 从 "id:[0-9]{4}" 中提取 "id"
     */
    private fun getVariableName(content: String): String {
        val colonIndex = content.indexOf(':')
        return if (colonIndex != -1) {
            content.substring(0, colonIndex).trim()
        } else {
            content.trim()
        }
    }
}