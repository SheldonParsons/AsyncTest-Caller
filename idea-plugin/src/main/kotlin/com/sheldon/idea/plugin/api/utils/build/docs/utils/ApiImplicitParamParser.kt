package com.sheldon.idea.plugin.api.utils.build.docs.utils
import com.intellij.psi.*
import com.sheldon.idea.plugin.api.service.SpringClassName
/**
 * Swagger ApiImplicitParam / ApiImplicitParams 解析器
 */
object ApiImplicitParamParser {
    fun parse(annotation: PsiAnnotation?): List<ApiImplicitParamInfo> {
        if (annotation == null) {
            return emptyList()
        }
        return when (annotation.qualifiedName) {
            SpringClassName.SWAGGER_API_IMPLICIT_PARAM ->
                listOfNotNull(parseSingle(annotation))
            SpringClassName.SWAGGER_API_IMPLICIT_PARAMS ->
                parseContainer(annotation)
            else -> emptyList()
        }
    }
    // ---------------------- 单个解析 ----------------------
    private fun parseSingle(annotation: PsiAnnotation): ApiImplicitParamInfo? {
        val name = annotation.getStringAttribute("name") ?: return null
        val dataTypeClass = annotation.getClassAttribute("dataTypeClass")
        val dataType = dataTypeClass ?: annotation.getStringAttribute("dataType") ?: ""
        return ApiImplicitParamInfo(
            name = name,
            value = annotation.getStringAttribute("value") ?: "",
            defaultValue = annotation.getStringAttribute("defaultValue") ?: "",
            allowableValues = annotation.getStringAttribute("allowableValues") ?: "",
            required = annotation.getBooleanAttribute("required"),
            access = annotation.getStringAttribute("access") ?: "",
            allowMultiple = annotation.getBooleanAttribute("allowMultiple"),
            dataType = dataType,
            paramType = annotation.getStringAttribute("paramType") ?: "",
            example = annotation.getStringAttribute("example") ?: "",
            examples = annotation.getExamples(),
            type = annotation.getStringAttribute("type") ?: "",
            format = annotation.getStringAttribute("format") ?: "",
            allowEmptyValue = annotation.getBooleanAttribute("allowEmptyValue"),
            readOnly = annotation.getBooleanAttribute("readOnly"),
            collectionFormat = annotation.getStringAttribute("collectionFormat") ?: ""
        )
    }
    // ---------------------- 容器解析 ----------------------
    private fun parseContainer(annotation: PsiAnnotation): List<ApiImplicitParamInfo> {
        val attr = annotation.findAttributeValue("value")
        return when (attr) {
            is PsiArrayInitializerMemberValue ->
                attr.initializers.mapNotNull { it ->
                    if (it != null && it is PsiAnnotation) {
                        return@mapNotNull parseSingle(it)
                    }
                    null
                }
            is PsiAnnotation ->
                listOfNotNull(parseSingle(attr))
            else -> emptyList()
        }
    }
    // ---------------------- Example 解析 ----------------------
    private fun PsiAnnotation.getExamples(): Map<String, String> {
        val attr = findAttributeValue("examples") as? PsiAnnotation ?: return emptyMap()
        val valueAttr = attr.findAttributeValue("value") as? PsiArrayInitializerMemberValue ?: return emptyMap()
        return valueAttr.initializers
            .mapNotNull { it as? PsiAnnotation }
            .mapNotNull {
                val mediaType = it.getStringAttribute("mediaType") ?: return@mapNotNull null
                val value = it.getStringAttribute("value") ?: ""
                mediaType to value
            }
            .toMap()
    }
    // ---------------------- PSI 工具函数 ----------------------
    private fun PsiAnnotation.getStringAttribute(name: String): String? =
        (findAttributeValue(name) as? PsiLiteralExpression)?.value as? String
    private fun PsiAnnotation.getBooleanAttribute(name: String): Boolean =
        (findAttributeValue(name) as? PsiLiteralExpression)?.value as? Boolean ?: false
    private fun PsiAnnotation.getClassAttribute(name: String): String? {
        val attr = findAttributeValue(name)
        return when (attr) {
            is PsiClassObjectAccessExpression ->
                attr.operand.type.canonicalText.removeSuffix(".class")
            is PsiReferenceExpression ->
                attr.referenceName
            else -> null
        }
    }
}
/**
 * 数据模型
 */
data class ApiImplicitParamInfo(
    val name: String = "",
    val value: String = "",
    val defaultValue: String = "",
    val allowableValues: String = "",
    val required: Boolean = false,
    val access: String = "",
    val allowMultiple: Boolean = false,
    val dataType: String = "",
    val paramType: String = "",
    val example: String = "",
    val examples: Map<String, String> = emptyMap(),
    val type: String = "",
    val format: String = "",
    val allowEmptyValue: Boolean = false,
    val readOnly: Boolean = false,
    val collectionFormat: String = ""
)
