package com.sheldon.idea.plugin.api.utils.build

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass

import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.utils.RouteKey
import com.sheldon.idea.plugin.api.utils.RouteRegistry
import com.sheldon.idea.plugin.api.utils.build.helper.ClassHelper

class BuildControllerNode(
    val module: Module,
    val project: Project,
) : TreeBuilder() {
    fun build(psiClass: PsiClass, pathPrefix: String, routeRegistry: RouteRegistry): ApiNode? {
        val classHelper = ClassHelper(module, project, psiClass)
        val classPath = "$pathPrefix.${psiClass.name}[2]"
        val classNode = makeClassNode(classHelper, psiClass, classPath)
        BuildMethodNode(module, project).build(
            classHelper,
            psiClass,
            classNode.treePath,
            classNode
        ) { methodNode: ApiNode ->
            val newMethodNode = routeRegistry.getApiNode(
                module.name,
                RouteKey(methodNode.method ?: "", methodNode.path ?: ""),
                psiClass
            )
            if (newMethodNode != null) {
                newMethodNode.treePath = "$classPath.${newMethodNode.name}[3]"
                classNode.addChild(newMethodNode)
            }
        }
        if (classNode.children.isNullOrEmpty()) return null
        classNode.classRequest = null
        return classNode
    }
}