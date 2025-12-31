package com.sheldon.idea.plugin.api.utils.build.resolver.method.parameter

import com.intellij.psi.*
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiUtil
import com.sheldon.idea.plugin.api.method.ParamLocation
import com.sheldon.idea.plugin.api.model.CodeType
import com.sheldon.idea.plugin.api.service.SpringClassName
import com.sheldon.idea.plugin.api.utils.TypeUtils
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.docs.DocInfo
import com.sheldon.idea.plugin.api.utils.build.docs.DocResolver
import com.sheldon.idea.plugin.api.utils.build.resolver.AnnotationResolver
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper
import com.sheldon.idea.plugin.api.utils.build.resolver.findAnnotationInHierarchy

class AnnotatedParameterResolver : MethodParameterResolver {
    override fun resolve(
        parameter: PsiParameter,
        method: PsiMethod,
        psiClass: PsiClass,
        implicitParams: MutableMap<String, DocInfo>,
        hasDocs: Boolean
    ): ParamAnalysisResult? {
        val requestBody = parameter.findAnnotationInHierarchy(SpringClassName.REQUEST_BODY_ANNOTATION)
        val (docInfo, _) = DocResolver().resolve(parameter, mutableMapOf(), CodeType.PARAM, hasDocs)
        val realType = TypeUtils.getRealTypeForMethod(method, parameter.type, psiClass)
        if (requestBody != null) {
            val isRequired = getBooleanAttribute(requestBody, SpringClassName.ATTR_REQUIRED, true)
            return ParamAnalysisResult(
                location = ParamLocation.BODY, name = "", t = realType, isRequired = isRequired, docInfo = docInfo
            )
        }
        val requestHeader = parameter.findAnnotationInHierarchy(SpringClassName.REQUEST_HEADER_ANNOTATION)
        if (requestHeader != null) {
            if (isHeaderContainer(realType)) {
                return null
            }
            val info = extractBasicInfo(requestHeader, parameter)
            return ParamAnalysisResult(
                location = ParamLocation.HEADER,
                name = info.name,
                t = realType,
                isRequired = info.required,
                defaultValue = info.defaultValue,
                docInfo = docInfo
            )
        }
        val cookieValue = parameter.findAnnotationInHierarchy(SpringClassName.COOKIE_VALUE_ANNOTATION)
        if (cookieValue != null) {
            val info = extractBasicInfo(cookieValue, parameter)
            return ParamAnalysisResult(
                location = ParamLocation.HEADER,
                name = info.name,
                t = realType,
                isRequired = info.required,
                defaultValue = info.defaultValue,
                docInfo = docInfo
            )
        }
        val requestParam = parameter.findAnnotationInHierarchy(SpringClassName.REQUEST_PARAM_ANNOTATION)
        if (requestParam != null) {
            val info = extractBasicInfo(requestParam, parameter)
            val isFile = ResolverHelper.isMultipartFile(realType)
            val location = if (isFile) ParamLocation.FORM_DATA else ParamLocation.QUERY
            return ParamAnalysisResult(
                location = location,
                name = info.name,
                t = realType,
                isRequired = info.required,
                defaultValue = info.defaultValue,
                docInfo = docInfo
            )
        }
        val requestPart = parameter.findAnnotationInHierarchy(SpringClassName.REQUEST_PART_ANNOTATION)
        if (requestPart != null) {
            val info = extractBasicInfo(requestPart, parameter)
            return ParamAnalysisResult(
                location = ParamLocation.FORM_DATA, name = info.name, t = realType, isRequired = info.required,
                docInfo = docInfo
            )
        }
        val modelAttribute = parameter.findAnnotationInHierarchy(SpringClassName.MODEL_ATTRIBUTE_ANNOTATION)
        val isFile = ResolverHelper.isMultipartFile(realType)
        if (isFile) return null
        if (modelAttribute != null) {
            return ParamAnalysisResult(
                location = ParamLocation.QUERY, name = parameter.name, t = realType, isRequired = false,
                docInfo = docInfo
            )
        }
        return null
    }

    private fun isHeaderContainer(type: PsiType): Boolean {
        val psiClass = PsiUtil.resolveClassInType(type) ?: return false
        val qName = psiClass.qualifiedName ?: return false
        if (qName == SpringClassName.CONTAINER_HTTP_HEADERS || qName == SpringClassName.CONTAINER_JAVA_UTIL_MAP || qName == SpringClassName.CONTAINER_MULTI_VALUE_MAP) {
            return true
        }
        if (InheritanceUtil.isInheritor(
                type, SpringClassName.CONTAINER_HTTP_HEADERS
            ) || InheritanceUtil.isInheritor(
                type, SpringClassName.CONTAINER_JAVA_UTIL_MAP
            ) || InheritanceUtil.isInheritor(type, SpringClassName.CONTAINER_MULTI_VALUE_MAP)
        ) {
            return true
        }
        return false
    }

    data class BasicInfo(val name: String, val required: Boolean, val defaultValue: String? = null)

    /**
     * 提取注解通用的 name, required, defaultValue 属性
     */
    private fun extractBasicInfo(annotation: PsiAnnotation, parameter: PsiParameter): BasicInfo {
        var name = AnnotationResolver.getAnnotationAttributeValues(annotation, SpringClassName.ATTR_VALUE).firstOrNull()
        if (name == null || name.isEmpty()) {
            name = AnnotationResolver.getAnnotationAttributeValues(annotation, SpringClassName.ATTR_NAME).firstOrNull()
        }
        if (name == null || name.isEmpty()) {
            name = parameter.name
        }
        val required = getBooleanAttribute(annotation, SpringClassName.ATTR_REQUIRED, true)
        var defaultValue =
            AnnotationResolver.getAnnotationAttributeValues(annotation, SpringClassName.ATTR_DEFAULT_VALUE)
                .firstOrNull()
        if (defaultValue == SpringClassName.VAL_DEFAULT_NONE) {
            defaultValue = null
        }
        return BasicInfo(name, required, defaultValue)
    }

    private fun getBooleanAttribute(annotation: PsiAnnotation, name: String, defaultVal: Boolean): Boolean {
        val value = annotation.findAttributeValue(name) ?: return defaultVal
        if (value.text == "true") return true
        if (value.text == "false") return false
        return defaultVal
    }
}