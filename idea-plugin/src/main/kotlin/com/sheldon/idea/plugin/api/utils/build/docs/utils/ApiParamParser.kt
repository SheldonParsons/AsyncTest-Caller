package com.sheldon.idea.plugin.api.utils.build.docs.utils
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiLiteralExpression
// ---------------------- 数据模型 ----------------------
data class ApiParamInfo(
    val name: String = "",
    val description: String = "",
    val defaultValue: String = "",
    val allowableValues: String = "",
    val required: Boolean = false,
    val hidden: Boolean = false,
    val example: String = "",
    val type: String = "",
    val format: String = "",
    val allowEmptyValue: Boolean = false,
    val readOnly: Boolean = false,
    val collectionFormat: String = "",
    val extensions: List<ApiExtension> = emptyList()
)
// ---------------------- 工具类 ----------------------
object ApiParamParser {
    fun parse(annotation: PsiAnnotation): ApiParamInfo {
        val name = annotation.getStringAttribute("name") ?: ""
        val description = annotation.getStringAttribute("value") ?: ""
        val defaultValue = annotation.getStringAttribute("defaultValue") ?: ""
        val allowableValues = annotation.getStringAttribute("allowableValues") ?: ""
        val required = annotation.getBooleanAttribute("required")
        val hidden = annotation.getBooleanAttribute("hidden")
        val example = annotation.getStringAttribute("example") ?: ""
        val type = annotation.getStringAttribute("type") ?: ""
        val format = annotation.getStringAttribute("format") ?: ""
        val allowEmptyValue = annotation.getBooleanAttribute("allowEmptyValue")
        val readOnly = annotation.getBooleanAttribute("readOnly")
        val collectionFormat = annotation.getStringAttribute("collectionFormat") ?: ""
        val extensions = annotation.getExtensions()
        return ApiParamInfo(
            name = name,
            description = description,
            defaultValue = defaultValue,
            allowableValues = allowableValues,
            required = required,
            hidden = hidden,
            example = example,
            type = type,
            format = format,
            allowEmptyValue = allowEmptyValue,
            readOnly = readOnly,
            collectionFormat = collectionFormat,
            extensions = extensions
        )
    }
    // ---------------------- 辅助解析函数 ----------------------
    private fun PsiAnnotation.getStringAttribute(name: String): String? =
        (findAttributeValue(name) as? PsiLiteralExpression)?.value as? String
    private fun PsiAnnotation.getBooleanAttribute(name: String): Boolean =
        (findAttributeValue(name) as? PsiLiteralExpression)?.value as? Boolean ?: false
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
