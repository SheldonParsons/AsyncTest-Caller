package com.sheldon.idea.plugin.api.utils.build.resolver.method.parameter

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameter
import com.intellij.psi.util.PsiUtil
import com.sheldon.idea.plugin.api.method.ParamLocation
import com.sheldon.idea.plugin.api.utils.TypeUtils
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper

class PojoResolver : MethodParameterResolver {

    override fun resolve(parameter: PsiParameter, method: PsiMethod, psiClass: PsiClass): ParamAnalysisResult? {
        // 不支持接口和抽象类
        val paramPsiClass = PsiUtil.resolveClassInType(parameter.type) ?: return null
        if (paramPsiClass.isInterface || paramPsiClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
            return null
        }
        val realType = TypeUtils.getRealTypeForMethod(method, parameter.type, psiClass)
        // PojoResolver 是责任链的最后一环。文件类型已经被MultipartFileResolver给过滤掉了，但还是做一个保险，避免顺序改变
        if (ResolverHelper.isMultipartFile(parameter.type)) {
            return ParamAnalysisResult(
                location = ParamLocation.FORM_DATA,
                name = parameter.name,
                t = parameter.type,
                isRequired = true
            )
        } else {
            return ParamAnalysisResult(
                location = ParamLocation.QUERY,
                name = "",
                t = realType,
                isRequired = false,
            )
        }
    }
}