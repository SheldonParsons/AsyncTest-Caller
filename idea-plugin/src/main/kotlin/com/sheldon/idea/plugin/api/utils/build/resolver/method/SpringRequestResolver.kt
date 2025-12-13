package com.sheldon.idea.plugin.api.utils.build.resolver.method

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.model.ApiRequest

class SpringRequestResolver {

    private val annotationResolver = SpringMethodAnnotationResolver()
    private val parameterResolver = SpringParameterResolver()

    fun resolver(
        method: PsiMethod,
        classNode: ApiNode,
        psiClass: PsiClass,
        module: Module,
        excludeParam: Boolean = false
    ): ApiRequest? {

        // 1. 获取注解静态信息 (Headers, Consumes)
        var apiRequest = annotationResolver.resolve(method) ?: return null
        classNode.classRequest?.let { req ->
            apiRequest = annotationResolver.mergeInfo(req, apiRequest)
        }
        if (apiRequest.method == null) {
            apiRequest.method = "get"
        }
        // 当前的annotationInfo还是不完整的，不包括body的内容，其他参数在后续也有可能改变
        if (excludeParam) return apiRequest
        // 2. 获取参数分析结果 (Params, Body, Dynamic Headers)
        val paramResults = parameterResolver.resolve(method, psiClass)
//        println("--- method resolved ---")
//        println("path: ${apiRequest.method}:${apiRequest.path}")
//        println("paramResults: $paramResults")
        // 3. 挂在请求内容到apiRequest
        DispatcherParameterResolver().analyze(apiRequest, paramResults, module)

        return apiRequest
    }
}