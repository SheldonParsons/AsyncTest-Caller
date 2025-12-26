package com.sheldon.idea.plugin.api.utils.build.resolver.method

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.sheldon.idea.plugin.api.method.AsyncTestVariableNode
import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.service.SpringClassName
import com.sheldon.idea.plugin.api.utils.build.resolver.AnnotationResolver
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper

class SpringMethodAnnotationResolver {
    /**
     * 解析方法注解上的静态配置
     */
    fun resolve(method: PsiMethod): ApiRequest? {
        var genericAnnotation: PsiAnnotation? = null
        var specificAnnotation: PsiAnnotation? = null

        val annotationMethod = AnnotationResolver.findMethodWithMapping(method) ?: return null

        for (annotation in annotationMethod.annotations) {
            val qName = annotation.qualifiedName ?: continue
            if (qName == SpringClassName.REQUEST_MAPPING_ANNOTATION) {
                genericAnnotation = annotation
            } else if (SpringClassName.SPRING_SINGLE_REQUEST_MAPPING_ANNOTATIONS.contains(qName) && specificAnnotation == null) {
                specificAnnotation = annotation
            }
        }
        if (genericAnnotation == null && specificAnnotation == null) return null
        val genericInfo = if (genericAnnotation != null) parseSingleAnnotation(genericAnnotation) else ApiRequest()
        val specificInfo = if (specificAnnotation != null) parseSingleAnnotation(specificAnnotation) else ApiRequest()
        return mergeInfo(genericInfo, specificInfo)
    }

    fun mergeInfo(genericInfo: ApiRequest, specificInfo: ApiRequest): ApiRequest {
        val method = specificInfo.method ?: genericInfo.method
        val path: String = ResolverHelper.combinePath(genericInfo.path, specificInfo.path)
        val headers: MutableList<AsyncTestVariableNode> =
            ResolverHelper.mergeHeadersOrParams(genericInfo.headers, specificInfo.headers) { it.name.lowercase() }
        val params: MutableList<AsyncTestVariableNode> =
            ResolverHelper.mergeHeadersOrParams(genericInfo.query, specificInfo.query, distinct = false)
        specificInfo.path = path
        specificInfo.method = method
        specificInfo.query = params
        specificInfo.headers = headers
        return specificInfo
    }

    companion object {
        fun parseSingleAnnotation(annotation: PsiAnnotation, ignoreMethod: Boolean = false): ApiRequest {
            var method: String? = null
            if (!ignoreMethod) {
                method = ResolverHelper.parseRequestMethod(annotation)?.name
            }
            val path = ResolverHelper.getPath(annotation)
            val headers = ResolverHelper.parseParamsOrHeaders(annotation, SpringClassName.ATTR_HEADERS) { k, v ->
                AsyncTestVariableNode(type = "string", name = k.lowercase(), defaultValue = v)
            }
            ResolverHelper.parseConsumes(annotation, SpringClassName.ATTR_CONSUMES) { value ->
                val contentTypeKey = SpringClassName.CONTENT_TYPE.lowercase()
                headers.removeIf { it.name == contentTypeKey }
                headers.add(
                    AsyncTestVariableNode(
                        type = "string", name = SpringClassName.CONTENT_TYPE, defaultValue = value
                    )
                )
            }
            val params = ResolverHelper.parseParamsOrHeaders(annotation, SpringClassName.ATTR_PARAMS) { k, v ->
                AsyncTestVariableNode(type = "string", name = k.lowercase(), defaultValue = v)
            }
            return ApiRequest(
                path = path, headers = headers, query = params, method = method
            )
        }
    }
}