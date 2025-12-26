package com.sheldon.idea.plugin.api.utils.build.resolver.method

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.resolver.method.parameter.*

class SpringParameterChain {
    private val resolvers = listOf(
        AnnotatedParameterResolver(),
        HttpEntityResolver(),
        MultipartFileResolver(),
        SimpleTypeResolver(),
        PojoResolver()
    )

    fun analyze(parameter: PsiParameter, method: PsiMethod, psiClass: PsiClass): ParamAnalysisResult? {
        var resolved: ParamAnalysisResult?
        for (resolver in resolvers) {
            resolved = resolver.resolve(parameter, method, psiClass)
            if (resolved != null) {
                return resolved
            }
        }
        return null
    }
}