package com.sheldon.idea.plugin.api.utils.build.resolver.method.parameter
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.docs.DocInfo
interface MethodParameterResolver {
    /**
     * 尝试解析参数
     * @return 如果能处理，返回 Result；如果不能处理（或应该忽略），返回 null
     */
    fun resolve(
        parameter: PsiParameter,
        method: PsiMethod,
        psiClass: PsiClass,
        implicitParams: MutableMap<String, DocInfo> = mutableMapOf(),
        hasDocs: Boolean = false
    ): ParamAnalysisResult?
}