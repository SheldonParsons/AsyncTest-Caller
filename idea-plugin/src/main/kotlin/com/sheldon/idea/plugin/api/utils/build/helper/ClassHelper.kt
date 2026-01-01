package com.sheldon.idea.plugin.api.utils.build.helper
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.sheldon.idea.plugin.api.utils.build.BaseHelper
class ClassHelper(val module: Module, val project: Project, val psiClass: PsiClass) : BaseHelper() {
    fun getMethods(): Array<PsiMethod> {
        return psiClass.methods
    }
}