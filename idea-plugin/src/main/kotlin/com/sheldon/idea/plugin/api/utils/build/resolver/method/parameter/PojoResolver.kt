package com.sheldon.idea.plugin.api.utils.build.resolver.method.parameter

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameter
import com.intellij.psi.util.PsiUtil
import com.sheldon.idea.plugin.api.method.ParamLocation
import com.sheldon.idea.plugin.api.model.CodeType
import com.sheldon.idea.plugin.api.utils.TypeUtils
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.docs.DocInfo
import com.sheldon.idea.plugin.api.utils.build.docs.DocResolver
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper

class PojoResolver : MethodParameterResolver {
    override fun resolve(
        parameter: PsiParameter,
        method: PsiMethod,
        psiClass: PsiClass,
        implicitParams: MutableMap<String, DocInfo>,
        hasDocs: Boolean
    ): ParamAnalysisResult? {
        val paramPsiClass = PsiUtil.resolveClassInType(parameter.type) ?: return null
        if (paramPsiClass.isInterface || paramPsiClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
            return null
        }
        val realType = TypeUtils.getRealTypeForMethod(method, parameter.type, psiClass)
        val (docInfo, _) = DocResolver().resolve(parameter, mutableMapOf(), CodeType.PARAM, hasDocs)
        if (ResolverHelper.isMultipartFile(parameter.type)) {
            return ParamAnalysisResult(
                location = ParamLocation.FORM_DATA,
                name = parameter.name,
                t = parameter.type,
                isRequired = true,
                docInfo = docInfo
            )
        } else {
            return ParamAnalysisResult(
                location = ParamLocation.QUERY,
                name = "",
                t = realType,
                isRequired = false,
                docInfo = docInfo
            )
        }
    }
}