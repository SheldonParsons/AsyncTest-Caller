package com.sheldon.idea.plugin.api.utils.build

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.utils.RouteKey
import com.sheldon.idea.plugin.api.utils.RouteRegistry
import com.sheldon.idea.plugin.api.utils.ScanSession
import com.sheldon.idea.plugin.api.utils.SpringConfigReader
import com.sheldon.idea.plugin.api.utils.build.helper.ClassHelper
import com.sheldon.idea.plugin.api.utils.build.helper.MethodHelper
import com.sheldon.idea.plugin.api.utils.build.lifecycle.AfterBuildRequest
import com.sheldon.idea.plugin.api.utils.build.resolver.AnnotationResolver

class MethodNodeBuilder(private val project: Project, val session: ScanSession) : TreeBuilder() {
    fun scan(): RouteRegistry {
        return runReadAction {
            val routerRegistry = RouteRegistry()
            val modules = ModuleManager.getInstance(project).modules
            for (module in modules) {
                println("module:${module.name}")
                val baseDir = BuildRootTree(module.project).getBaseDir(module)
                println("baseDir:$baseDir")
                if (baseDir != null) {
                    collectRecursively(baseDir, module, routerRegistry, SpringConfigReader.getSpringBaseUrl(module))
                }
            }
            routerRegistry
        }
    }

    fun scanModule(module: Module): RouteRegistry {
        return runReadAction {
            val routerRegistry = RouteRegistry()
            val baseDir = BuildRootTree(module.project).getBaseDir(module)
            if (baseDir != null) {
                collectRecursively(baseDir, module, routerRegistry, SpringConfigReader.getSpringBaseUrl(module))
            }
            return@runReadAction routerRegistry
        }
    }

    fun scanDir(module: Module, dir: PsiDirectory): RouteRegistry {
        return runReadAction {
            val routerRegistry = RouteRegistry()
            collectRecursively(dir, module, routerRegistry, SpringConfigReader.getSpringBaseUrl(module))
            return@runReadAction routerRegistry
        }
    }

    fun scanClass(module: Module, psiClass: PsiClass): RouteRegistry {
        return runReadAction {
            val routerRegistry = RouteRegistry()
            collectClass(module, psiClass, routerRegistry, prefix = SpringConfigReader.getSpringBaseUrl(module))
            return@runReadAction routerRegistry
        }
    }

    fun scanMethod(module: Module, psiMethod: PsiMethod, psiClass: PsiClass, classNode: ApiNode): RouteRegistry {
        return runReadAction {
            val routerRegistry = RouteRegistry()
            collectMethod(
                module,
                psiClass,
                psiMethod,
                classNode,
                routerRegistry,
                prefix = SpringConfigReader.getSpringBaseUrl(module)
            )
            return@runReadAction routerRegistry
        }
    }

    private fun collectRecursively(
        currentDir: PsiDirectory,
        module: Module,
        routerRegistry: RouteRegistry,
        prefix: String = "http://localhost:8080"
    ) {
        for (file in currentDir.files) {
            if (file is PsiJavaFile) {
                for (psiClass in file.classes) {
                    if (isController(psiClass)) {
                        collectClass(module, psiClass, routerRegistry, prefix)
                    }
                }
            }
        }
        for (subDir in currentDir.subdirectories) {
            collectRecursively(subDir, module, routerRegistry, prefix)
        }
    }

    private fun collectClass(module: Module, psiClass: PsiClass, routerRegistry: RouteRegistry, prefix: String) {
        val classHelper = ClassHelper(module, project, psiClass)
        val methods = classHelper.getMethods()
        val classNode = makeClassNode(classHelper, psiClass, "")
        for (psiMethod in methods) {
            if (!collectMethod(module, psiClass, psiMethod, classNode, routerRegistry, prefix)) continue
        }
    }

    private fun collectMethod(
        module: Module,
        psiClass: PsiClass,
        psiMethod: PsiMethod,
        classNode: ApiNode,
        routerRegistry: RouteRegistry,
        prefix: String = ""
    ): Boolean {
        val methodHelper = MethodHelper(module, project, psiClass, psiMethod)
        if (AnnotationResolver.isMappingMethod(psiMethod)) {
            val methodNode = makeMethodNode(
                methodHelper,
                psiMethod,
                module.name,
                classNode
            ) { methodHelper, request ->
                AfterBuildRequest(request).execute(
                    methodHelper.project,
                    methodHelper.module,
                    saveMock = session.saveMock,
                    prefix
                )
            }
            if (methodNode != null) {
                routerRegistry.register(
                    RouteKey(methodNode.method ?: "", methodNode.path ?: ""),
                    methodNode,
                    psiClass,
                    module.name
                )
            }
        }
        return true
    }
}