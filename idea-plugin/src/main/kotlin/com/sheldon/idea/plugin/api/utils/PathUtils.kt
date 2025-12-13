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
                // 进入变量解析模式
                val variableContent = extractBalancedBraceContent(springPath, i)
                if (variableContent != null) {
                    // variableContent 是花括号里面的内容，比如 "id:[0-9]{4}"

                    // 提取变量名：取第一个冒号前的部分
                    val varName = getVariableName(variableContent)
                    result.append("{{").append(varName).append("}}")

                    // i 跳过整个 {xxx} 块
                    i += variableContent.length + 2 // +2 是因为要把外层的 {} 加上
                    continue
                }
            }

            // 普通字符直接追加
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
        // 从 startIndex 开始扫描
        for (j in startIndex until text.length) {
            val char = text[j]
            if (char == '{') {
                balance++
            } else if (char == '}') {
                balance--
            }

            // 当平衡归零时，说明找到了最外层的闭合括号
            if (balance == 0) {
                // 返回中间的内容 (去掉首尾的 {})
                return text.substring(startIndex + 1, j)
            }
        }
        // 括号没闭合（语法错误），原样返回 null，外层会当作普通字符处理
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