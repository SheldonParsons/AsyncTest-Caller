package com.sheldon.idea.plugin.api.utils.build.resolver.method.parameter

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.sheldon.idea.plugin.api.method.ParamLocation
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper

class MultipartFileResolver : MethodParameterResolver {

    override fun resolve(parameter: PsiParameter, method: PsiMethod, psiClass: PsiClass): ParamAnalysisResult? {
        // 1. 检查类型是否是 MultipartFile
        if (ResolverHelper.isMultipartFile(parameter.type)) {
            // 2. 隐式的文件上传
            return ParamAnalysisResult(
                location = ParamLocation.FORM_DATA, // 必须是 FORM_DATA
                name = parameter.name,              // 使用参数名
                t = parameter.type,
                isRequired = true                   // 默认必填
            )
        }
        return null
    }
}