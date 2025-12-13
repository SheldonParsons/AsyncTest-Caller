package com.sheldon.idea.plugin.api.utils.build.resolver.method.parameter

import com.intellij.psi.*
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiUtil
import com.sheldon.idea.plugin.api.method.ParamLocation
import com.sheldon.idea.plugin.api.service.SpringClassName
import com.sheldon.idea.plugin.api.utils.TypeUtils
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper

class AnnotatedParameterResolver : MethodParameterResolver {

    override fun resolve(parameter: PsiParameter, method: PsiMethod, psiClass: PsiClass): ParamAnalysisResult? {
        // 1. 检查 @RequestBody (Body JSON)
        val requestBody = parameter.getAnnotation(SpringClassName.REQUEST_BODY_ANNOTATION)
        val realType = TypeUtils.getRealTypeForMethod(method, parameter.type, psiClass)
        if (requestBody != null) {
            // @RequestBody 不需要提取 name (body 是整体)，默认 required=true
            val isRequired = getBooleanAttribute(requestBody, SpringClassName.ATTR_REQUIRED, true)
            return ParamAnalysisResult(
                location = ParamLocation.BODY,
                name = "", // Body 整体通常不需要 key，除非是 map key
                t = realType,
                isRequired = isRequired
            )
        }

        // 3. 检查 @RequestHeader (Header)
        val requestHeader = parameter.getAnnotation(SpringClassName.REQUEST_HEADER_ANNOTATION)
        if (requestHeader != null) {
            if (isHeaderContainer(realType)) {
                // 不处理容器类型
                return null
            }
            val info = extractBasicInfo(requestHeader, parameter)
            return ParamAnalysisResult(
                location = ParamLocation.HEADER,
                name = info.name,
                t = realType,
                isRequired = info.required,
                defaultValue = info.defaultValue
            )
        }

        // 检查 @CookieValue (作为 Header 处理)
        val cookieValue = parameter.getAnnotation(SpringClassName.COOKIE_VALUE_ANNOTATION)
        if (cookieValue != null) {
            val info = extractBasicInfo(cookieValue, parameter)
            return ParamAnalysisResult(
                location = ParamLocation.HEADER, // 按照你的要求，归类为 Header
                name = info.name,            // Cookie 的名字 (例如 "JSESSIONID")
                t = realType,
                isRequired = info.required,  // CookieValue 默认 required=true
                defaultValue = info.defaultValue
            )
        }

        // 4. 检查 @RequestParam (Query 或 Form-Data)
        val requestParam = parameter.getAnnotation(SpringClassName.REQUEST_PARAM_ANNOTATION)
        if (requestParam != null) {
            val info = extractBasicInfo(requestParam, parameter)

            // 🟢 关键逻辑：决定是 Query 还是 Form-Data
            // 如果是文件类型，即使标注了 @RequestParam，也是 Form-Data
            val isFile = ResolverHelper.isMultipartFile(realType)
            val location = if (isFile) ParamLocation.FORM_DATA else ParamLocation.QUERY

            return ParamAnalysisResult(
                location = location,
                name = info.name,
                t = realType,
                isRequired = info.required,
                defaultValue = info.defaultValue
            )
        }

        // 5. 检查 @RequestPart (Form-Data)
        val requestPart = parameter.getAnnotation(SpringClassName.REQUEST_PART_ANNOTATION)
        if (requestPart != null) {
            val info = extractBasicInfo(requestPart, parameter)
            return ParamAnalysisResult(
                location = ParamLocation.FORM_DATA,
                name = info.name,
                t = realType,
                isRequired = info.required
            )
        }

        // 6. 检查 @ModelAttribute (Query / Form-Data)
        // 注意：@ModelAttribute 比较特殊，通常意味着这是一个对象，需要展开
        // 这里我们先暂时标记为 QUERY，后续如果需要支持复杂对象展开，可以在 SpringRequestAnalyzer 中二次处理
        val modelAttribute = parameter.getAnnotation(SpringClassName.MODEL_ATTRIBUTE_ANNOTATION)
        val isFile = ResolverHelper.isMultipartFile(realType)
        if (isFile) return null
        if (modelAttribute != null) {
            // ModelAttribute 的 name 属性通常是给 Model 用的 key，而不是 HTTP 参数 key
            // 但如果它修饰的是简单类型，它就是参数名。
            // 这里简化处理：作为 Query 参数返回
            return ParamAnalysisResult(
                location = ParamLocation.QUERY,
                name = parameter.name, // 通常忽略注解里的 value，直接用参数名
                t = realType,
                isRequired = false // ModelAttribute 通常非必填
            )
        }

        return null // 没找到任何注解，交给下一层
    }

    private fun isHeaderContainer(type: PsiType): Boolean {
        val psiClass = PsiUtil.resolveClassInType(type) ?: return false
        val qName = psiClass.qualifiedName ?: return false

        if (qName == SpringClassName.CONTAINER_HTTP_HEADERS ||
            qName == SpringClassName.CONTAINER_JAVA_UTIL_MAP ||
            qName == SpringClassName.CONTAINER_MULTI_VALUE_MAP
        ) {
            return true
        }
        if (InheritanceUtil.isInheritor(
                type,
                SpringClassName.CONTAINER_HTTP_HEADERS
            ) || InheritanceUtil.isInheritor(
                type,
                SpringClassName.CONTAINER_JAVA_UTIL_MAP
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
        // 1. 提取 Name
        // 优先取 value/name，如果没有，回退使用参数名
        var name = ResolverHelper.getAnnotationAttributeValues(annotation, SpringClassName.ATTR_VALUE).firstOrNull()
        if (name == null) {
            name = ResolverHelper.getAnnotationAttributeValues(annotation, SpringClassName.ATTR_NAME).firstOrNull()
        }
        if (name == null) {
            name = parameter.name
        }

        // 2. 提取 Required (默认为 true)
        val required = getBooleanAttribute(annotation, SpringClassName.ATTR_REQUIRED, true)

        // 3. 提取 DefaultValue
        var defaultValue =
            ResolverHelper.getAnnotationAttributeValues(annotation, SpringClassName.ATTR_DEFAULT_VALUE).firstOrNull()
        // Spring 有个特殊的 "ValueConstants.DEFAULT_NONE"，如果等于这个，说明没设默认值
        if (defaultValue == SpringClassName.VAL_DEFAULT_NONE) {
            defaultValue = null
        }

        return BasicInfo(name, required, defaultValue)
    }

    private fun getBooleanAttribute(annotation: PsiAnnotation, name: String, defaultVal: Boolean): Boolean {
        val value = annotation.findAttributeValue(name) ?: return defaultVal
        // 简单处理字面量 true/false
        if (value.text == "true") return true
        if (value.text == "false") return false
        return defaultVal
    }
}