package com.sheldon.idea.plugin.api.utils.build.docs.extractor

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.sheldon.idea.plugin.api.model.CodeType
import com.sheldon.idea.plugin.api.utils.build.docs.DocExtractor
import com.sheldon.idea.plugin.api.utils.build.docs.DocInfo
import com.sheldon.idea.plugin.api.utils.build.docs.ExtractionContext
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper.Companion.cleanComment

class JavadocExtractor : DocExtractor {
    override fun getOrder() = 10

    override fun extract(
        context: ExtractionContext,
        currentDoc: DocInfo,
        codeType: CodeType,
        hasDocs: Boolean
    ) {
        val element: PsiElement = context.targetElement
        val (javaDocTitle, javaDocDesc) = parseJavadoc(element, codeType) // 伪代码：解析第一行为title，其余为desc
        if (javaDocTitle.isNotEmpty()) {
            currentDoc.merge(javaDocTitle, javaDocDesc)
        }
    }

    private fun parseJavadoc(element: PsiElement, codeType: CodeType): Pair<String, String> {
        if (codeType == CodeType.PARAM) {
            return Pair(getFieldComment(element), "")
        } else {
            return getElementComment(element)
        }
    }

    fun getFieldComment(element: PsiElement?): String {
        if (element == null) return ""
        val comments = mutableListOf<String>()
        if (element is PsiDocCommentOwner && element.docComment != null) {
            val rawText = element.docComment!!
                .descriptionElements
                .joinToString("") { it.text }

            return normalizeJavadoc(rawText)
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n")
        }
        var child = element.firstChild
        while (child != null) {
            when (child) {
                is PsiComment -> comments.add(cleanComment(child.text))
                is PsiWhiteSpace -> {
                    // ignore
                }

                else -> break
            }
            child = child.nextSibling
        }
        if (comments.isNotEmpty()) {
            return comments.joinToString("\n")
        }
        var prev = element.prevSibling
        var safetyCount = 0
        while (prev != null && safetyCount < 20) {
            if (prev is PsiComment) {
                comments.add(0, cleanComment(prev.text))
            } else if (prev is PsiWhiteSpace) {
                if (prev.text.count { it == '\n' } > 1) {
                    break
                }
            } else if (prev is PsiAnnotation) {
                continue
            } else {
                break
            }
            prev = prev.prevSibling
            safetyCount++
        }
        return comments.joinToString("\n")
    }


    fun getElementComment(element: PsiElement): Pair<String, String> {
        if (element !is PsiDocCommentOwner) return "" to ""
        val docComment = element.docComment ?: return "" to ""
        val rawText = docComment.descriptionElements.joinToString("") { it.text }
        val cleanText = normalizeJavadoc(rawText)
        val validLines = cleanText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val alias = if (validLines.firstOrNull() == null) "" else validLines.first()
        val desc = if (validLines.size > 1) {
            validLines.drop(1).joinToString("\n")
        } else {
            ""
        }
        return alias to desc
    }

    /**
     * 强力清洗函数
     */
    private fun normalizeJavadoc(text: String): String {
        var result = text
        result = result.replace(Regex("(?m)^[\\s]*\\*+"), "").replace(Regex("^/\\*+"), "").replace(Regex("\\*+/$"), "")
        result = result.replace(Regex("(?i)</?(p|br|div|li|ul|h\\d)[^>]*>"), "\n")
        result = result.replace(Regex("<[^>]+>"), "")
        result = result.replace(Regex("\\{@\\w+\\s+(.*?)\\}"), "$1")
        result = result.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
            .replace("&quot;", "\"")
        return result
    }
}