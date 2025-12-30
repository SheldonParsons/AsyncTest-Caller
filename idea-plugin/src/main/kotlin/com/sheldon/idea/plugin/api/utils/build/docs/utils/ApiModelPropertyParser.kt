package com.sheldon.idea.plugin.api.utils.build.docs.utils

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiReferenceExpression

// ---------------------- 数据模型 ----------------------
data class ApiModelPropertyInfo(
    val title: String = "",
    val name: String = "",
    val allowableValues: String = "",
    val notes: String = "",
    val dataType: String = "",
    val required: Boolean = false,
    val hidden: Boolean = false,
    val example: String = "",
    val accessMode: String = "",
    val reference: String = "",
    val allowEmptyValue: Boolean = false,
    val extensions: List<ApiExtension> = emptyList()
)

// ---------------------- 工具类 ----------------------
object ApiModelPropertyParser {

    fun parse(annotation: PsiAnnotation): ApiModelPropertyInfo {
        val title = annotation.getStringAttribute("value") ?: ""
        val name = annotation.getStringAttribute("name") ?: ""
        val allowableValues = annotation.getStringAttribute("allowableValues") ?: ""
        val notes = annotation.getStringAttribute("notes") ?: ""
        val dataType = annotation.getStringAttribute("dataType") ?: ""
        val required = annotation.getBooleanAttribute("required")
        val hidden = annotation.getBooleanAttribute("hidden")
        val example = annotation.getStringAttribute("example") ?: ""
        val accessMode = annotation.getEnumAttribute("accessMode") ?: ""
        val reference = annotation.getStringAttribute("reference") ?: ""
        val allowEmptyValue = annotation.getBooleanAttribute("allowEmptyValue")
        val extensions = annotation.getExtensions()

        return ApiModelPropertyInfo(
            title = title,
            name = name,
            allowableValues = allowableValues,
            notes = notes,
            dataType = dataType,
            required = required,
            hidden = hidden,
            example = example,
            accessMode = accessMode,
            reference = reference,
            allowEmptyValue = allowEmptyValue,
            extensions = extensions
        )
    }

    // ---------------------- 辅助解析函数 ----------------------

    private fun PsiAnnotation.getStringAttribute(name: String): String? =
        (findAttributeValue(name) as? PsiLiteralExpression)?.value as? String

    private fun PsiAnnotation.getBooleanAttribute(name: String): Boolean =
        (findAttributeValue(name) as? PsiLiteralExpression)?.value as? Boolean ?: false

    private fun PsiAnnotation.getEnumAttribute(name: String): String? {
        val attr = findAttributeValue(name)
        return when (attr) {
            is PsiReferenceExpression -> attr.referenceName
            else -> null
        }
    }

    private fun PsiAnnotation.getExtensions(): List<ApiExtension> {
        val attr = findAttributeValue("extensions")
        return when (attr) {
            is PsiArrayInitializerMemberValue ->
                attr.initializers.mapNotNull { parseExtension(it as? PsiAnnotation) }

            is PsiAnnotation ->
                listOfNotNull(parseExtension(attr))

            else -> emptyList()
        }
    }

    private fun parseExtension(annotation: PsiAnnotation?): ApiExtension? {
        annotation ?: return null
        val propsAttr =
            annotation.findAttributeValue("properties") as? PsiArrayInitializerMemberValue ?: return null
        val firstProp = propsAttr.initializers.firstOrNull() as? PsiAnnotation ?: return null
        val name = firstProp.getStringAttribute("name") ?: return null
        val value = firstProp.getStringAttribute("value") ?: ""
        return ApiExtension(name, value)
    }
}
