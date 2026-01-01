package com.sheldon.idea.plugin.api.utils.build.resolver
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiBinaryExpression
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.JavaConstantExpressionEvaluator
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiUtil
import com.sheldon.idea.plugin.api.service.SpringClassName
class ResolverHelper {
    companion object {
        fun parseRequestMethod(annotation: PsiAnnotation): SpringClassName.RequestMethod? {
            val qName = annotation.qualifiedName ?: return null
            if (SpringClassName.METHOD_ANNOTATION_MAP.containsKey(qName)) {
                val method = SpringClassName.METHOD_ANNOTATION_MAP[qName]!!
                return listOf(method).firstOrNull()
            }
            if (qName == SpringClassName.REQUEST_MAPPING_ANNOTATION) {
                val valueElement = annotation.findAttributeValue(SpringClassName.ATTR_METHOD)
                    ?: return null
                return resolveMethodEnumValues(valueElement)
            }
            return null
        }
        fun isMultipartFile(type: PsiType): Boolean {
            if (isSingleFile(type)) return true
            if (type is com.intellij.psi.PsiArrayType) {
                val itemType = type.componentType
                return isSingleFile(itemType)
            }
            val itemType = PsiUtil.extractIterableTypeParameter(type, false)
            if (itemType != null) {
                return isSingleFile(itemType)
            }
            return false
        }
        private fun isSingleFile(type: PsiType): Boolean {
            return ResolverHelper.isInheritor(type, SpringClassName.MULTI_PART_FILE) || ResolverHelper.isInheritor(
                type,
                SpringClassName.JAVAX_PART
            ) || ResolverHelper.isInheritor(type, SpringClassName.JAKARTA_PART)
        }
        fun isInheritor(type: PsiType, targetFqn: String): Boolean {
            val psiClass = PsiUtil.resolveClassInType(type) ?: return false
            if (psiClass.qualifiedName == targetFqn) {
                return true
            }
            return InheritanceUtil.isInheritor(psiClass, targetFqn)
        }
        private fun resolveMethodEnumValues(element: PsiElement): SpringClassName.RequestMethod? {
            val methods = mutableListOf<SpringClassName.RequestMethod>()
            fun extract(item: PsiElement) {
                if (item is PsiReferenceExpression) {
                    val name = item.referenceName
                    val methodEnum = SpringClassName.RequestMethod.from(name)
                    if (methodEnum != null) {
                        methods.add(methodEnum)
                    }
                }
            }
            if (element is PsiArrayInitializerMemberValue) {
                element.initializers.forEach { extract(it) }
            } else {
                extract(element)
            }
            return methods.firstOrNull()
        }
        private fun generateNotEqualValue(original: String): String {
            if (original.isEmpty()) return "not_empty"
            val chars = original.toCharArray()
            val lastIndex = chars.lastIndex
            val lastChar = chars[lastIndex]
            chars[lastIndex] = if (lastChar.code > 33) {
                (lastChar.code - 1).toChar()
            } else {
                (lastChar.code + 1).toChar()
            }
            return String(chars)
        }
        fun <T> mergeHeadersOrParams(
            baseList: MutableList<T>?,
            overlayList: MutableList<T>?,
            distinct: Boolean = true,
            keySelector: ((T) -> String)? = null
        ): MutableList<T> {
            val result = mutableListOf<T>()
            if (!distinct) {
                if (baseList != null) result.addAll(baseList)
                if (overlayList != null) result.addAll(overlayList)
                return result
            }
            if (keySelector == null) {
                val set = LinkedHashSet<T>()
                if (baseList != null) set.addAll(baseList)
                if (overlayList != null) set.addAll(overlayList)
                result.addAll(set)
                return result
            }
            val map = LinkedHashMap<String, T>()
            baseList?.forEach { item ->
                val key = keySelector(item)
                if (key.isNotEmpty()) map[key] = item
            }
            overlayList?.forEach { item ->
                val key = keySelector(item)
                if (key.isNotEmpty()) map[key] = item
            }
            result.addAll(map.values)
            return result
        }
        fun <T> addOrUpdateElement(
            list: MutableList<T>,
            element: T,
            overwrite: Boolean = true,
            keySelector: ((T) -> String)? = null
        ) {
            if (!overwrite) {
                list.add(element)
                return
            }
            var index = -1
            if (keySelector != null) {
                val key = keySelector(element)
                if (key.isNotEmpty()) {
                    index = list.indexOfFirst { keySelector(it) == key }
                }
            } else {
                index = list.indexOf(element)
            }
            if (index != -1) {
                list[index] = element
            } else {
                list.add(element)
            }
        }
        fun combinePath(base: String?, sub: String?): String {
            val safeBase = base?.trim() ?: ""
            val safeSub = sub?.trim() ?: ""
            if (safeBase.isEmpty() && safeSub.isEmpty()) {
                return ""
            }
            val cleanBase = safeBase.removeSuffix("/")
            val cleanSub = safeSub.removePrefix("/")
            val combined = if (cleanBase.isEmpty()) {
                cleanSub
            } else if (cleanSub.isEmpty()) {
                cleanBase
            } else {
                "$cleanBase/$cleanSub"
            }
            return if (!combined.startsWith("/")) {
                "/$combined"
            } else {
                combined
            }
        }
        fun getElementComment(element: PsiElement?): String {
            if (element == null) return ""
            val comments = mutableListOf<String>()
            if (element is PsiDocCommentOwner && element.docComment != null) {
                return element.docComment!!.descriptionElements.joinToString("") { it.text }
                    .trim().replace(Regex("\\n\\s*\\*"), "\n").trim()
            }
            var child = element.firstChild
            while (child != null) {
                if (child is PsiComment) {
                    comments.add(cleanComment(child.text))
                } else if (child is PsiWhiteSpace) {
                } else {
                    break
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
                } else {
                    break
                }
                prev = prev.prevSibling
                safetyCount++
            }
            return comments.joinToString("\n")
        }
        fun cleanComment(text: String): String {
            return text.trim()
                .replace(Regex("^//\\s*"), "")
                .replace(Regex("^/\\*+\\s*"), "")
                .replace(Regex("\\s*\\*+/$"), "")
                .trim()
        }
        fun debugPsiStructure(element: PsiElement) {
            println("DEBUG START: 正在检查字段 [${element.text.substringBefore("\n").take(20)}...]")
            var prev = element.prevSibling
            var count = 0
            while (prev != null && count < 10) {
                val className = prev::class.java.simpleName
                val content = prev.text.replace("\n", "\\n").replace("\r", "\\r")
                println("   Previous[$count] -> 类型: $className | 内容: '$content'")
                prev = prev.prevSibling
                count++
            }
            println("DEBUG END\n")
        }
    }
}