package com.sheldon.idea.plugin.api.utils.build.resolver.method

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.resolver.method.parameter.*

class SpringParameterChain {

    // 解析器链 (顺序非常重要！)
    private val resolvers = listOf(
        AnnotatedParameterResolver(), // 1. 显式注解优先
        HttpEntityResolver(),         // 2. 解析HttpEntity相关类型
        MultipartFileResolver(),      // 3. 特殊类型 (文件)
        SimpleTypeResolver(),         // 4. 隐式简单类型
        PojoResolver()                // 5. 隐式复杂对象 (兜底)
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