package com.sheldon.idea.plugin.api.utils.build.resolver.method

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.sheldon.idea.plugin.api.method.AsyncTestVariableNode
import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.service.SpringClassName
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper

class SpringMethodAnnotationResolver {
    /**
     * 解析方法注解上的静态配置
     */
    fun resolve(method: PsiMethod): ApiRequest? {
        // 1. 识别两种类型的注解
        var genericAnnotation: PsiAnnotation? = null // @RequestMapping
        var specificAnnotation: PsiAnnotation? = null // @GetMapping, @PostMapping...

        for (annotation in method.annotations) {
            val qName = annotation.qualifiedName ?: continue

            if (qName == SpringClassName.REQUEST_MAPPING_ANNOTATION) {
                genericAnnotation = annotation
            } else if (SpringClassName.SPRING_SINGLE_REQUEST_MAPPING_ANNOTATIONS.contains(qName)) {
                specificAnnotation = annotation
            }
        }

        if (genericAnnotation == null && specificAnnotation == null) return null

        // 2. 分别解析
        val genericInfo =
            if (genericAnnotation != null) parseSingleAnnotation(genericAnnotation) else ApiRequest()
        val specificInfo =
            if (specificAnnotation != null) parseSingleAnnotation(specificAnnotation) else ApiRequest()

        // 3. 合并逻辑 (Merge)
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

            // Path
            val path = ResolverHelper.getPath(annotation)

            // Headers (headers + consumes)
            val headers = ResolverHelper.parseParamsOrHeaders(annotation, SpringClassName.ATTR_HEADERS) { k, v ->
                // TODO:这里需求修改，需要知道header的类型
                AsyncTestVariableNode(type = "string", name = k.lowercase(), defaultValue = v)
            }

            ResolverHelper.parseConsumes(annotation, SpringClassName.ATTR_CONSUMES) { value ->
                headers.add(
                    AsyncTestVariableNode(
                        type = "string",
                        name = SpringClassName.CONTENT_TYPE,
                        defaultValue = value
                    )
                )
            }

            // params
            val params = ResolverHelper.parseParamsOrHeaders(annotation, SpringClassName.ATTR_PARAMS) { k, v ->
                AsyncTestVariableNode(type = "string", name = k, defaultValue = v)
            }

            return ApiRequest(
                path = path,
                headers = headers,
                query = params,
                method = method
            )
        }
    }

}