package com.sheldon.idea.plugin.api.utils.build

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.sheldon.idea.plugin.api.utils.RouteKey
import com.sheldon.idea.plugin.api.utils.RouteRegistry
import com.sheldon.idea.plugin.api.utils.build.helper.ClassHelper
import com.sheldon.idea.plugin.api.utils.build.helper.MethodHelper

class MethodNodeBuilder(private val project: Project) : TreeBuilder() {

    fun scan(): RouteRegistry {
        return runReadAction {
            val routerRegistry = RouteRegistry()
            val modules = ModuleManager.getInstance(project).modules
            for (module in modules) {
                val contentEntries = ModuleRootManager.getInstance(module).contentEntries
                    .flatMap { it.sourceFolders.asList() }
                for (folder in contentEntries) {
                    val url = folder.url
                    if (!url.contains("${module.name}/src/main/java")) continue
                    val rootDir = PsiManager.getInstance(project).findDirectory(folder.file ?: continue)
                        ?: continue
                    val baseDir = findBasePackageDirectory(rootDir) ?: rootDir
                    collectRecursively(baseDir, module, routerRegistry)
                }
            }
            routerRegistry
        }
    }

    private fun collectRecursively(currentDir: PsiDirectory, module: Module, routerRegistry: RouteRegistry) {
        // 1. 遍历当前目录下的文件
        for (file in currentDir.files) {
            // 只要 Java 文件
            if (file is PsiJavaFile) {
                for (psiClass in file.classes) {
                    if (isController(psiClass)) {
                        val classHelper = ClassHelper(module, project, psiClass)
                        val methods = classHelper.getMethods()
                        val classNode = makeClassNode(classHelper, psiClass, "")
                        for (psiMethod in methods) {
                            val containingClass = psiMethod.containingClass ?: continue
                            val methodHelper = MethodHelper(module, project, psiClass, psiMethod)
                            if (!methodHelper.shouldIncludeMethod(psiClass, containingClass)) {
                                continue
                            }
                            if (isMappingMethod(psiMethod)) {
                                val methodNode = makeMethodNode(
                                    methodHelper,
                                    psiMethod,
                                    psiClass,
                                    module.name,
                                    classNode,
                                    module.name
                                )
                                if (methodNode != null) {
                                    routerRegistry.register(
                                        RouteKey(methodNode.method ?: "", methodNode.path ?: ""),
                                        methodNode,
                                        psiClass,
                                        module.name
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. 递归进入子目录
        for (subDir in currentDir.subdirectories) {
            collectRecursively(subDir, module, routerRegistry)
        }
    }
}