package com.sheldon.idea.plugin.api.utils.build.resolver.method

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult

class SpringParameterResolver {

    /**
     * 解析方法的所有入参
     */
    fun resolve(method: PsiMethod, psiClass: PsiClass): List<ParamAnalysisResult> {
        val results = mutableListOf<ParamAnalysisResult>()

        for (parameter in method.parameterList.parameters) {
            if (SpringIgnoredTypeResolver.isIgnored(parameter)) {
                continue
            }
            val result = analyzeParameter(parameter, method, psiClass)
            if (result != null) {
                results.add(result)
            }
        }
        return results
    }

    /**
     * 分析单个参数
     */
    private fun analyzeParameter(parameter: PsiParameter, method: PsiMethod, psiClass: PsiClass): ParamAnalysisResult? {
        return SpringParameterChain().analyze(parameter, method, psiClass)
    }
}