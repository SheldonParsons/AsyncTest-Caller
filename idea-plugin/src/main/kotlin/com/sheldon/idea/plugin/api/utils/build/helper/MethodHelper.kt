package com.sheldon.idea.plugin.api.utils.build.helper

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiMethod
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.utils.build.BaseHelper
import com.sheldon.idea.plugin.api.utils.build.resolver.method.SpringRequestResolver

class MethodHelper(val module: Module, val project: Project, val psiClass: PsiClass, val psiMethod: PsiMethod) :
    BaseHelper() {
    fun getMethodNodeCoreInfo(classNode: ApiNode, excludeParam: Boolean = false): ApiRequest? {
        return SpringRequestResolver().resolver(psiMethod, classNode, psiClass, module, excludeParam)
    }

    fun shouldIncludeMethod(currentClass: PsiClass, methodClass: PsiClass): Boolean {
        if (currentClass.hasModifierProperty(PsiModifier.ABSTRACT) || currentClass.isInterface) {
            return false
        }
        if (currentClass == methodClass) {
            return true
        }
        if (methodClass.hasModifierProperty(PsiModifier.ABSTRACT) || methodClass.isInterface) {
            return false
        }
        return false
    }
}