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
        if (type is PsiArrayType) {
            return type.componentType
        }
        val resolveResult = PsiUtil.resolveGenericsClassInType(type)
        resolveResult.element ?: return null
        return resolveResult.substitutor.substitutionMap.values.firstOrNull()
    }
}