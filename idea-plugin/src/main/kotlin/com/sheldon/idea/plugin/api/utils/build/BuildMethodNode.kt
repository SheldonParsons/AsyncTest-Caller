package com.sheldon.idea.plugin.api.utils.build
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.utils.build.helper.ClassHelper
import com.sheldon.idea.plugin.api.utils.build.helper.MethodHelper
import com.sheldon.idea.plugin.api.utils.build.resolver.AnnotationResolver
class BuildMethodNode(
    val module: Module,
    val project: Project,
) : TreeBuilder() {
    fun build(
        classHelper: ClassHelper,
        psiClass: PsiClass,
        classPath: String,
        classNode: ApiNode,
        callback: (ApiNode) -> Unit
    ) {
        val methods = classHelper.getMethods()
        for (psiMethod in methods) {
            val methodHelper = MethodHelper(module, project, psiClass, psiMethod)
            if (AnnotationResolver.isMappingMethod(psiMethod)) {
                val methodNode =
                    makeMethodExcludeParam(methodHelper, psiMethod, classPath, classNode)
                if (methodNode != null) callback(methodNode)
            }
        }
    }
}
