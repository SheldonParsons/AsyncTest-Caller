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
            classNode.tree_path,
            classNode
        ) { methodNode: ApiNode ->
            val newMethodNode = routeRegistry.getApiNode(
                module.name,
                RouteKey(methodNode.method ?: "", methodNode.path ?: ""),
                psiClass
            )
            if (newMethodNode != null) {
                newMethodNode.tree_path = "$classPath.${newMethodNode.name}[3]"
                classNode.addChild(newMethodNode)
            }
        }
        if (classNode.children.isEmpty()) return null
//        classNode.classRequest = null
        return classNode
    }

    fun buildMethod(psiClass: PsiClass, pathPrefix: String, routeRegistry: RouteRegistry, onResult: (ApiNode) -> Unit) {
        val classHelper = ClassHelper(module, project, psiClass)
        val classNode = makeClassNode(classHelper, psiClass, pathPrefix)
        BuildMethodNode(module, project).build(
            classHelper,
            psiClass,
            classNode.tree_path,
            classNode
        ) { methodNode: ApiNode ->
            val newMethodNode = routeRegistry.getApiNode(
                module.name,
                RouteKey(method = methodNode.method ?: "", fullUrl = methodNode.path ?: ""),
                psiClass
            )
            newMethodNode?.let {
                it.tree_path = "$pathPrefix.${it.name}[3]"
                onResult(it)
            }
        }
    }
}