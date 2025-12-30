package com.sheldon.idea.plugin.api.utils.build.resolver.method

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.docs.DocInfo
import com.sheldon.idea.plugin.api.utils.build.resolver.method.parameter.*

class SpringParameterChain {
    private val resolvers = listOf(
        AnnotatedParameterResolver(),
        HttpEntityResolver(),
        MultipartFileResolver(),
        SimpleTypeResolver(),
        PojoResolver()
    )

    fun analyze(
        parameter: PsiParameter,
        method: PsiMethod,
        psiClass: PsiClass,
        implicitParams: MutableMap<String, DocInfo> = mutableMapOf(),
        hasDocs: Boolean = false
    ): ParamAnalysisResult? {
        var resolved: ParamAnalysisResult?
        for (resolver in resolvers) {
            resolved = resolver.resolve(parameter, method, psiClass, implicitParams = implicitParams, hasDocs = hasDocs)
            if (resolved != null) {
                return resolved
            }
        }
        return null
    }
}