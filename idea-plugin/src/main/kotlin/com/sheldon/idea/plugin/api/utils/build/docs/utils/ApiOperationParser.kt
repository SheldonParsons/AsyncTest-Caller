package com.sheldon.idea.plugin.api.utils.build.docs.utils
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiReferenceExpression
// ---------------------- 数据模型 ----------------------
data class ApiResponseHeader(
    val name: String,
    val response: String
)
data class ApiExtension(
    val name: String,
    val value: String
)
data class ApiOperationInfo(
    val title: String = "",
    val desc: String = "",
    val tags: List<String> = emptyList(),
    val responseType: String? = "",
    val responseContainer: String? = "",
    val responseReference: String? = "",
    val httpMethod: String? = "",
    val hidden: Boolean = false,
    val authorizations: List<String> = emptyList(),
    val responseHeaders: List<ApiResponseHeader> = emptyList(),
    val code: Int = 200,
    val extensions: List<ApiExtension> = emptyList()
)
// ---------------------- 工具类 ----------------------
object ApiOperationParser {
    fun parse(annotation: PsiAnnotation): ApiOperationInfo {
        val title = annotation.getStringAttribute("value") ?: ""
        val desc = annotation.getStringAttribute("notes") ?: ""
        val tags = annotation.getStringArrayAttribute("tags").filter { it.isNotBlank() }
        val responseType = annotation.getClassAttribute("response")
        val responseContainer = annotation.getStringAttribute("responseContainer")
        val responseReference = annotation.getStringAttribute("responseReference")
        val httpMethod = annotation.getStringAttribute("httpMethod")
        val hidden = annotation.getBooleanAttribute("hidden")
        val authorizations = annotation.getAuthorizations()
        val responseHeaders = annotation.getResponseHeaders()
        val code = annotation.getIntAttribute("code") ?: 200
        val extensions = annotation.getExtensions()
        return ApiOperationInfo(
            title = title,
            desc = desc,
            tags = tags,
            responseType = responseType,
            responseContainer = responseContainer,
            responseReference = responseReference,
            httpMethod = httpMethod,
            hidden = hidden,
            authorizations = authorizations,
            responseHeaders = responseHeaders,
            code = code,
            extensions = extensions
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
            is PsiArrayInitializerMemberValue -> attr.initializers.mapNotNull { (it as? PsiLiteralExpression)?.value as? String }
            is PsiLiteralExpression -> listOf(attr.value as? String ?: return emptyList())
            else -> emptyList()
        }
    }
    private fun PsiAnnotation.getClassAttribute(name: String): String? {
        val attr = findAttributeValue(name)
        return when (attr) {
            is PsiReferenceExpression -> attr.referenceName
            else -> null
        }
    }
    private fun PsiAnnotation.getAuthorizations(): List<String> {
        val attr = findAttributeValue("authorizations")
        return when (attr) {
            is PsiArrayInitializerMemberValue -> attr.initializers.mapNotNull { parseAuthorization(it as? PsiAnnotation) }
            is PsiAnnotation -> listOfNotNull(parseAuthorization(attr))
            else -> emptyList()
        }
    }
    private fun parseAuthorization(annotation: PsiAnnotation?): String? {
        annotation ?: return null
        return annotation.getStringAttribute("value")
    }
    private fun PsiAnnotation.getResponseHeaders(): List<ApiResponseHeader> {
        val attr = findAttributeValue("responseHeaders")
        return when (attr) {
            is PsiArrayInitializerMemberValue -> attr.initializers.mapNotNull { parseResponseHeader(it as? PsiAnnotation) }
            is PsiAnnotation -> listOfNotNull(parseResponseHeader(attr))
            else -> emptyList()
        }
    }
    private fun parseResponseHeader(annotation: PsiAnnotation?): ApiResponseHeader? {
        annotation ?: return null
        val name = annotation.getStringAttribute("name") ?: return null
        val response = annotation.getClassAttribute("response") ?: "Void"
        return ApiResponseHeader(name, response)
    }
    private fun PsiAnnotation.getExtensions(): List<ApiExtension> {
        val attr = findAttributeValue("extensions")
        return when (attr) {
            is PsiArrayInitializerMemberValue -> attr.initializers.mapNotNull { parseExtension(it as? PsiAnnotation) }
            is PsiAnnotation -> listOfNotNull(parseExtension(attr))
            else -> emptyList()
        }
    }
    private fun parseExtension(annotation: PsiAnnotation?): ApiExtension? {
        annotation ?: return null
        val propsAttr = annotation.findAttributeValue("properties") as? PsiArrayInitializerMemberValue ?: return null
        val firstProp = propsAttr.initializers.firstOrNull() as? PsiAnnotation ?: return null
        val name = firstProp.getStringAttribute("name") ?: return null
        val value = firstProp.getStringAttribute("value") ?: ""
        return ApiExtension(name, value)
    }
}
