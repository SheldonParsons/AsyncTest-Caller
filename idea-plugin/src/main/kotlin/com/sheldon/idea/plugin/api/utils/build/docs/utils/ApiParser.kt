package com.sheldon.idea.plugin.api.utils.build.docs.utils

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiLiteralExpression

// ---------------------- 数据模型 ----------------------
data class ApiInfo(
    var value: String = "",
    val tags: List<String> = emptyList(),
    val description: String = "",
    val basePath: String = "",
    val position: Int = 0,
    val produces: String = "",
    val consumes: String = "",
    val protocols: String = "",
    val authorizations: List<String> = emptyList(),
    val hidden: Boolean = false,
    var qualifiedName: String = ""
)

// ---------------------- 工具类 ----------------------
object ApiParser {

    fun parse(annotation: PsiAnnotation): ApiInfo {
        val value = annotation.getStringAttribute("value") ?: ""
        val tags = annotation.getStringArrayAttribute("tags").filter { it.isNotBlank() }
        val description = annotation.getStringAttribute("description") ?: ""
        val basePath = annotation.getStringAttribute("basePath") ?: ""
        val position = annotation.getIntAttribute("position") ?: 0
        val produces = annotation.getStringAttribute("produces") ?: ""
        val consumes = annotation.getStringAttribute("consumes") ?: ""
        val protocols = annotation.getStringAttribute("protocols") ?: ""
        val authorizations = annotation.getAuthorizations()
        val hidden = annotation.getBooleanAttribute("hidden")

        return ApiInfo(
            value = value,
            tags = tags,
            description = description,
            basePath = basePath,
            position = position,
            produces = produces,
            consumes = consumes,
            protocols = protocols,
            authorizations = authorizations,
            hidden = hidden
        )
    }

    // ---------------------- 辅助解析函数 ----------------------

    private fun PsiAnnotation.getStringAttribute(name: String): String? =
        (findAttributeValue(name) as? PsiLiteralExpression)?.value as? String

    private fun PsiAnnotation.getIntAttribute(name: String): Int? =
        (findAttributeValue(name) as? PsiLiteralExpression)?.value as? Int

    private fun PsiAnnotation.getBooleanAttribute(name: String): Boolean =
        (findAttributeValue(name) as? PsiLiteralExpression)?.value as? Boolean ?: false

    private fun PsiAnnotation.getStringArrayAttribute(name: String): List<String> {
        val attr = findAttributeValue(name)
        return when (attr) {
            is PsiArrayInitializerMemberValue ->
                attr.initializers.mapNotNull { (it as? PsiLiteralExpression)?.value as? String }

            is PsiLiteralExpression ->
                listOfNotNull(attr.value as? String)

            else -> emptyList()
        }
    }

    private fun PsiAnnotation.getAuthorizations(): List<String> {
        val attr = findAttributeValue("authorizations")
        return when (attr) {
            is PsiArrayInitializerMemberValue ->
                attr.initializers.mapNotNull { (it as? PsiAnnotation)?.getStringAttribute("value") }

            is PsiAnnotation ->
                listOfNotNull(attr.getStringAttribute("value"))

            else -> emptyList()
        }
    }
}
