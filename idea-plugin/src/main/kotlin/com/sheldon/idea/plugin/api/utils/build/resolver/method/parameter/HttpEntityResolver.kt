package com.sheldon.idea.plugin.api.utils.build.resolver.method.parameter

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import com.sheldon.idea.plugin.api.method.ParamLocation
import com.sheldon.idea.plugin.api.service.SpringClassName
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper

class HttpEntityResolver : MethodParameterResolver {

    override fun resolve(parameter: PsiParameter, method: PsiMethod, psiClass: PsiClass): ParamAnalysisResult? {
        val type = parameter.type
        if (ResolverHelper.isInheritor(type, SpringClassName.HTTP_ENTITY) || ResolverHelper.isInheritor(
                type,
                SpringClassName.REQUEST_ENTITY
            )
        ) {
            // 核心逻辑：提取泛型 T
            // HttpEntity<UserDto> -> 提取出 UserDto
            var bodyType: PsiType? = null
            if (type is PsiClassType) {
                val parameters = type.parameters
                if (parameters.isNotEmpty()) {
                    bodyType = parameters[0]
                }
            }
            if (bodyType == null) {
                return null
            }


            return ParamAnalysisResult(
                location = ParamLocation.BODY, // 视为 JSON Body
                name = "",
                t = bodyType,
                isRequired = true
            )
        }
        return null
    }
}