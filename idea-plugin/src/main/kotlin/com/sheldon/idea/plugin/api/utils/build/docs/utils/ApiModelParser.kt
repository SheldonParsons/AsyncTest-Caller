package com.sheldon.idea.plugin.api.utils.build.docs.utils

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiReferenceExpression

// ---------------------- 数据模型 ----------------------
data class ApiModelInfo(
    val name: String = "",
    val description: String = "",
    val parent: String? = null,
    val discriminator: String = "",
    val subTypes: List<String> = emptyList(),
    val reference: String = ""
)

// ---------------------- 工具类 ----------------------
object ApiModelParser {

    fun parse(annotation: PsiAnnotation): ApiModelInfo {
        val name = annotation.getStringAttribute("value") ?: ""
        val description = annotation.getStringAttribute("description") ?: ""
        val parent = annotation.getClassAttribute("parent")
        val discriminator = annotation.getStringAttribute("discriminator") ?: ""
        val subTypes = annotation.getClassArrayAttribute("subTypes")
        val reference = annotation.getStringAttribute("reference") ?: ""

        return ApiModelInfo(
            name = name,
            description = description,
            parent = parent,
            discriminator = discriminator,
            subTypes = subTypes,
            reference = reference
        )
    }

    // ---------------------- 辅助解析函数 ----------------------

    private fun PsiAnnotation.getStringAttribute(name: String): String? =
        (findAttributeValue(name) as? PsiLiteralExpression)?.value as? String

    private fun PsiAnnotation.getClassAttribute(name: String): String? {
        val attr = findAttributeValue(name)
        return when (attr) {
            is PsiReferenceExpression -> attr.referenceName
            else -> null
        }
    }

    private fun PsiAnnotation.getClassArrayAttribute(name: String): List<String> {
        val attr = findAttributeValue(name)
        return when (attr) {
            is PsiArrayInitializerMemberValue ->
                attr.initializers.mapNotNull { (it as? PsiReferenceExpression)?.referenceName }

            is PsiReferenceExpression ->
                listOf(attr.referenceName ?: return emptyList())

            else -> emptyList()
        }
    }
}
