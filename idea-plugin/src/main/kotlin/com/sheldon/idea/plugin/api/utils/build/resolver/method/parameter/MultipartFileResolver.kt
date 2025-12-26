package com.sheldon.idea.plugin.api.utils.build.resolver.method.parameter

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.sheldon.idea.plugin.api.method.ParamLocation
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper

class MultipartFileResolver : MethodParameterResolver {
    override fun resolve(parameter: PsiParameter, method: PsiMethod, psiClass: PsiClass): ParamAnalysisResult? {
        if (ResolverHelper.isMultipartFile(parameter.type)) {
            return ParamAnalysisResult(
                location = ParamLocation.FORM_DATA,
                name = parameter.name,
                t = parameter.type,
                isRequired = true
            )
        }
        return null
    }
}