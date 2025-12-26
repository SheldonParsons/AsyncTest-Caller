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

    fun buildDir(
        rootDir: PsiDirectory,
        rootPrefix: String,
        nextBuild: (PsiClass, String) -> ApiNode?
    ): ApiNode {
        return recursiveBuildDirNode(rootDir, rootPrefix, nextBuild)
    }

    private fun recursiveBuildDirNode(
        currentDir: PsiDirectory,
        pathPrefix: String,
        nextBuild: (PsiClass, String) -> ApiNode?
    ): ApiNode {
        val currentNode = makeDirNode(currentDir, "$pathPrefix.${currentDir.name}")
        for (subDir in currentDir.subdirectories) {
            val subPath = if (pathPrefix.isBlank()) subDir.name else "$pathPrefix.${subDir.name}"
            val childDirNode = recursiveBuildDirNode(
                currentDir = subDir,
                pathPrefix = subPath,
                nextBuild = nextBuild
            )
            if (childDirNode.children.isNotEmpty()) {
                currentNode.addChild(childDirNode)
            }
        }
        for (file in currentDir.files) {
            if (file is PsiJavaFile) {
                for (psiClass in file.classes) {
                    if (isController(psiClass)) {
                        val classNode = nextBuild(psiClass, currentNode.tree_path)
                        if (classNode != null) {
                            currentNode.addChild(classNode)
                        }
                    }
                }
            }
        }
        return currentNode
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
                pathPrefix = dirNode.tree_path,
                nextBuild = nextBuild
            )
            if (dirNode.children.isNotEmpty()) {
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