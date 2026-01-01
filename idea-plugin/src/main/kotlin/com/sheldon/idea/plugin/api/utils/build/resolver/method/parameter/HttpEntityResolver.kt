package com.sheldon.idea.plugin.api.utils.build.resolver.method.parameter
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import com.sheldon.idea.plugin.api.method.ParamLocation
import com.sheldon.idea.plugin.api.model.CodeType
import com.sheldon.idea.plugin.api.service.SpringClassName
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.docs.DocInfo
import com.sheldon.idea.plugin.api.utils.build.docs.DocResolver
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper
class HttpEntityResolver : MethodParameterResolver {
    override fun resolve(
        parameter: PsiParameter,
        method: PsiMethod,
        psiClass: PsiClass,
        implicitParams: MutableMap<String, DocInfo>,
        hasDocs: Boolean
    ): ParamAnalysisResult? {
        val type = parameter.type
        if (ResolverHelper.isInheritor(type, SpringClassName.HTTP_ENTITY) || ResolverHelper.isInheritor(
                type,
                SpringClassName.REQUEST_ENTITY
            )
        ) {
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
            val (docInfo, _) = DocResolver().resolve(parameter, mutableMapOf(), CodeType.PARAM, hasDocs)
            return ParamAnalysisResult(
                location = ParamLocation.BODY,
                name = "",
                t = bodyType,
                isRequired = true,
                docInfo = docInfo
            )
        }
        return null
    }
}