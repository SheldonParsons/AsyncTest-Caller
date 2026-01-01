package com.sheldon.idea.plugin.api.utils.build
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*
object PsiPathResolver {
    data class PathSegment(
        val name: String,
        val type: Int
    )
    fun resolve(project: Project, pathStr: String): Any? {
        val segments = parsePath(pathStr)
        if (segments.isEmpty()) return null
        val moduleSegment = segments[0]
        if (moduleSegment.type != 0) return null
        val module = ModuleManager.getInstance(project).findModuleByName(moduleSegment.name) ?: return null
        if (segments.size == 1) return module
        var currentElement: Any = module
        for (i in 1 until segments.size) {
            val segment = segments[i]
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
    private fun findInModuleRoots(module: Module, segment: PathSegment): Any? {
        val rootDir = BuildRootTree(module.project).getBaseDir(module) ?: return null
        return findInDirectory(rootDir, segment)
    }
    private fun findInDirectory(dir: PsiDirectory, segment: PathSegment): Any? {
        return when (segment.type) {
            1 -> {
                dir.findSubdirectory(segment.name)
            }
            2 -> {
                var psiClass = JavaDirectoryService.getInstance().getClasses(dir).find { it.name == segment.name }
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
    private fun findInClass(psiClass: PsiClass, segment: PathSegment): Any? {
        if (segment.type == 3) {
            return psiClass.findMethodsByName(segment.name, false).firstOrNull()
        }
        return null
    }
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