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
        // 1. 【核心变化】先创建当前目录自己的节点 (作为 Root 或当前层级的父节点)
        // 注意：这里假设 makeDirNode 能够处理 currentDir 和 pathPrefix
        val currentNode = makeDirNode(currentDir, "$pathPrefix.${currentDir.name}")

        // 2. 遍历子目录
        for (subDir in currentDir.subdirectories) {
            // 计算子目录的 path (保持你原有的拼接逻辑)
            // 注意处理 pathPrefix 为空的情况，避免出现 ".child"
            val subPath = if (pathPrefix.isBlank()) subDir.name else "$pathPrefix.${subDir.name}"

            // 3. 【递归调用】接收递归返回回来的子节点
            val childDirNode = recursiveBuildDirNode(
                currentDir = subDir,
                pathPrefix = subPath,
                nextBuild = nextBuild
            )

            // 4. 【挂载】如果子目录有内容，才挂载到当前节点下 (保持你原有的判空逻辑)
            if (childDirNode.children.isNotEmpty()) {
                currentNode.addChild(childDirNode)
            }
        }

        // 5. 遍历当前目录下的文件
        for (file in currentDir.files) {
            if (file is PsiJavaFile) {
                for (psiClass in file.classes) {
                    if (isController(psiClass)) {
                        // 这里传入 currentNode.treePath 或者 pathPrefix 都可以，看你 nextBuild 具体的需要
                        val classNode = nextBuild(psiClass, currentNode.tree_path)
                        if (classNode != null) {
                            currentNode.addChild(classNode)
                        }
                    }
                }
            }
        }

        // 6. 返回构建好的当前节点
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