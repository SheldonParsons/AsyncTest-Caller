package com.sheldon.idea.plugin.api.utils.build.resolver.method.request_part

import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiUtil
import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult

interface RequestPartResolver {
    fun push(variable: ParamAnalysisResult, apiRequest: ApiRequest): ApiRequest

    fun extractMapValueType(type: PsiType): PsiType? {
        val resolveResult = PsiUtil.resolveGenericsClassInType(type)
        val psiClass = resolveResult.element ?: return null

        // Map 接口通常有两个泛型参数 <K, V>
        // 我们通过 typeParameters 获取参数定义的数组
        val typeParams = psiClass.typeParameters
        if (typeParams.size == 2) {
            return resolveResult.substitutor.substitute(typeParams[1])
        }

        return null
    }

    /**
     * 辅助：提取数组/集合的元素类型
     */
    fun extractArrayComponentType(type: PsiType): PsiType? {
        // 情况 A: 数组 String[]
        if (type is PsiArrayType) {
            return type.componentType
        }

        // 情况 B: 集合 List<String>
        val resolveResult = PsiUtil.resolveGenericsClassInType(type)
        resolveResult.element ?: return null // 如果解析不出类（比如是 int），直接返回 null
        // 尝试获取泛型参数
        return resolveResult.substitutor.substitutionMap.values.firstOrNull()
    }

}