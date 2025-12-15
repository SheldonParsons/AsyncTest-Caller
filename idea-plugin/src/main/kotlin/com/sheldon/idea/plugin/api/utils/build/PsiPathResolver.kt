package com.sheldon.idea.plugin.api.utils.build

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.*

object PsiPathResolver {

    data class PathSegment(
        val name: String,
        val type: Int // 0=Module, 1=Dir, 2=Class, 3=Method
    )

    /**
     * 核心方法：根据路径字符串找到 PSI 元素
     * @return 可能是 Module, PsiDirectory, PsiClass, 或 PsiMethod
     */
    fun resolve(project: Project, pathStr: String): Any? {
        val segments = parsePath(pathStr)
        if (segments.isEmpty()) return null

        // 1. 第一步：必须找到 Module (type=0)
        val moduleSegment = segments[0]
        if (moduleSegment.type != 0) return null // 路径必须以 Module 开头

        val module = ModuleManager.getInstance(project).findModuleByName(moduleSegment.name) ?: return null

        // 如果路径只有一段 (e.g. "my-module[0]")，直接返回 Module
        if (segments.size == 1) return module

        // 2. 准备递归：当前上下文元素
        var currentElement: Any = module

        // 3. 遍历后续片段
        for (i in 1 until segments.size) {
            val segment = segments[i]
            // 根据当前元素类型，决定怎么找下一个元素
            val nextElement = when (currentElement) {
                is Module -> findInModuleRoots(currentElement, segment)
                is PsiDirectory -> findInDirectory(currentElement, segment)
                is PsiClass -> findInClass(currentElement, segment)
                else -> null
            }
            if (nextElement == null) {
                println("PSI 解析中断: 在 ${currentElement.javaClass.simpleName} 中找不到 ${segment.name}[${segment.type}]")
                return null
            }
            currentElement = nextElement
        }
        return currentElement
    }

    // ========================================================================
    // 内部查找逻辑
    // ========================================================================

    /**
     * Context: Module -> 寻找 -> Directory (type=1) 或 Class (type=2)
     * 说明：Module 下面有多个 Source Root (src/main/java, src/test/java)，需要遍历查找
     */
    private fun findInModuleRoots(module: Module, segment: PathSegment): Any? {
        val psiManager = PsiManager.getInstance(module.project)
        // 获取模块所有的源码根目录 (VirtualFile)
        val sourceRoots = ModuleRootManager.getInstance(module).sourceRoots

        for (rootVirtualFile in sourceRoots) {
            val rootDir = psiManager.findDirectory(rootVirtualFile) ?: continue

            // 尝试在这个根目录下找
            val found = findInDirectory(rootDir, segment)
            if (found != null) return found
        }
        return null
    }

    /**
     * Context: Directory -> 寻找 -> Directory (type=1) 或 Class (type=2)
     */
    private fun findInDirectory(dir: PsiDirectory, segment: PathSegment): Any? {
        return when (segment.type) {
            1 -> { // 找子目录
                dir.findSubdirectory(segment.name)
            }

            2 -> { // 找类
                // 优先使用 Java 专用 API 找类 (能处理 .java 也能处理 .class)
                // 这样能避免去匹配 "BaseExtends.java" 这种带后缀的文件名
                var psiClass = JavaDirectoryService.getInstance().getClasses(dir).find { it.name == segment.name }

                // 如果找不到 (可能是 Kotlin 类或其他文件)，尝试找文件再转 Class
                if (psiClass == null) {
                    val file = dir.files.find { it.virtualFile.nameWithoutExtension == segment.name }
                    if (file is PsiClassOwner) {
                        psiClass = file.classes.firstOrNull { it.name == segment.name }
                    }
                }
                psiClass
            }

            else -> null
        }
    }

    /**
     * Context: Class -> 寻找 -> Method (type=3)
     */
    private fun findInClass(psiClass: PsiClass, segment: PathSegment): Any? {
        if (segment.type == 3) {
            // 寻找方法
            // 注意：如果存在重载 (Overloading)，findMethodsByName 会返回数组
            // 这里我们默认取第一个。如果你需要精确匹配，需要在路径中带上参数签名
            return psiClass.findMethodsByName(segment.name, false).firstOrNull()
        }
        // 如果你需要支持内部类，可以在这里加 type=2 的判断
        // if (segment.type == 2) return psiClass.findInnerClassByName(segment.name, false)

        return null
    }

    // ========================================================================
    // 路径解析器 (复用你之前的逻辑)
    // ========================================================================
    private fun parsePath(pathStr: String): List<PathSegment> {
        return pathStr.split(".").mapNotNull { segmentStr ->
            val match = Regex("""^(.*)\[(\d+)\]$""").find(segmentStr)
            if (match != null) {
                val (name, type) = match.destructured
                PathSegment(name, type.toInt())
            } else {
                null
            }
        }
    }
}