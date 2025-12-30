package com.sheldon.idea.plugin.api.utils.build.resolver.method.parameter

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.sheldon.idea.plugin.api.method.ParamLocation
import com.sheldon.idea.plugin.api.model.CodeType
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.docs.DocInfo
import com.sheldon.idea.plugin.api.utils.build.docs.DocResolver
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper

class MultipartFileResolver : MethodParameterResolver {
    override fun resolve(
        parameter: PsiParameter,
        method: PsiMethod,
        psiClass: PsiClass,
        implicitParams: MutableMap<String, DocInfo>,
        hasDocs: Boolean
    ): ParamAnalysisResult? {
        if (ResolverHelper.isMultipartFile(parameter.type)) {
            val (docInfo, _) = DocResolver().resolve(parameter, mutableMapOf(), CodeType.PARAM, hasDocs)
            return ParamAnalysisResult(
                location = ParamLocation.FORM_DATA,
                name = parameter.name,
                t = parameter.type,
                isRequired = true,
                docInfo = docInfo
            )
        }
        return null
    }
}