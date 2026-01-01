package com.sheldon.idea.plugin.api.utils.build.resolver.method
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.utils.build.docs.DocInfo
class SpringRequestResolver {
    private val annotationResolver = SpringMethodAnnotationResolver()
    private val parameterResolver = SpringParameterResolver()
    fun resolver(
        method: PsiMethod,
        classNode: ApiNode,
        psiClass: PsiClass,
        module: Module,
        excludeParam: Boolean = false,
        implicitParams: MutableMap<String, DocInfo> = mutableMapOf(),
        hasDocs: Boolean = false
    ): ApiRequest? {
        var apiRequest = annotationResolver.resolve(method) ?: return null
        classNode.classRequest?.let { req ->
            apiRequest = annotationResolver.mergeInfo(req, apiRequest)
        }
        if (apiRequest.method == null) {
            apiRequest.method = "get"
        }
        if (excludeParam) return apiRequest
        val paramResults =
            parameterResolver.resolve(method, psiClass, implicitParams = implicitParams, hasDocs = hasDocs)
        DispatcherParameterResolver().analyze(
            apiRequest,
            paramResults,
            module,
            implicitParams = implicitParams,
            hasDocs = hasDocs
        )
        return apiRequest
    }
}