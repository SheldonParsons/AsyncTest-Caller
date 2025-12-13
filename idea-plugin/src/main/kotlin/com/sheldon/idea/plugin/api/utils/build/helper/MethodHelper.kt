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
        // 3. 如果定义的方法是【抽象类】或【接口】 -> 跳过
        if (currentClass.hasModifierProperty(PsiModifier.ABSTRACT) || currentClass.isInterface) {
            return false
        }
        // 1. 如果方法就是在这个类里定义的 -> 保留，到此已经确定，当前方法是属于父类的了
        if (currentClass == methodClass) {
            return true
        }

        // 2. 如果定义方法的父类是【抽象类】或【接口】 -> 保留
        if (methodClass.hasModifierProperty(PsiModifier.ABSTRACT) || methodClass.isInterface) {
            return true
        }
        // 4. 如果定义方法的父类是【具体类 (Concrete Class)】-> 不保留，由父类自行创建
        return false
    }
}