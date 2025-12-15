package com.sheldon.idea.plugin.api.utils.build

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiJavaFile
import com.sheldon.idea.plugin.api.model.ApiNode
import com.intellij.openapi.project.Project

class BuildDirectoryTree(
    val module: Module,
    val project: Project,
) : TreeBuilder() {

    fun build(
        rootDir: PsiDirectory,
        rootNode: ApiNode,
        rootPrefix: String,
        nextBuild: (PsiClass, String) -> ApiNode?
    ) {
        recursiveBuild(rootDir, rootNode, rootPrefix, nextBuild)
    }

    private fun recursiveBuild(
        currentDir: PsiDirectory,
        parentNode: ApiNode,
        pathPrefix: String,
        nextBuild: (PsiClass, String) -> ApiNode?
    ) {
        for (subDir in currentDir.subdirectories) {
            val currentPath = "$pathPrefix.${subDir.name}"
            val dirNode = makeDirNode(subDir, currentPath)
            recursiveBuild(
                currentDir = subDir,
                parentNode = dirNode,
                pathPrefix = dirNode.treePath,
                nextBuild = nextBuild
            )

            if (!dirNode.children.isNullOrEmpty()) {
                parentNode.addChild(dirNode)
            }
        }

        for (file in currentDir.files) {
            if (file is PsiJavaFile) {
                for (psiClass in file.classes) {
                    if (isController(psiClass)) {
                        val classNode = nextBuild(psiClass, pathPrefix)
                        if (classNode != null) {
                            parentNode.addChild(classNode)
                        }
                    }
                }
            }
        }
    }
}