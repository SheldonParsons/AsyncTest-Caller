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
        var apiRequest = annotationResolver.resolve(method) ?: return null
        classNode.classRequest?.let { req ->
            apiRequest = annotationResolver.mergeInfo(req, apiRequest)
        }
        if (apiRequest.method == null) {
            apiRequest.method = "get"
        }
        if (excludeParam) return apiRequest
        val paramResults = parameterResolver.resolve(method, psiClass)
        DispatcherParameterResolver().analyze(apiRequest, paramResults, module)
        return apiRequest
    }
}