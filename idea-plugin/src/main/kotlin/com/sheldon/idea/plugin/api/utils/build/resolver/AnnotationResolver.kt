package com.sheldon.idea.plugin.api.utils.build.resolver

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner

object AnnotationResolver {


    /**
     * 【入口 1】查找类上的注解
     * 策略：
     * 1. 注解方向：支持元注解递归
     * 2. 继承方向：支持向上查找父类 (Class Inheritance)
     *
     * @return 找到的那个注解实例 (如果是元注解，返回的是深层的那个目标注解)
     */
    fun resolveForClass(psiClass: PsiClass, targetAnnotations: Collection<String>): PsiAnnotation? {
        var currentClass: PsiClass? = psiClass

        // 循环向上查找父类
        while (currentClass != null && currentClass.qualifiedName != "java.lang.Object") {
            // 在当前类上查找
            val found = resolveForElement(currentClass, targetAnnotations)
            if (found != null) {
                return found
            }
            // 继续找父类
            currentClass = currentClass.superClass
        }
        return null
    }

    /**
     * 【入口 2】查找方法上的注解
     * 策略：
     * 1. 注解方向：支持元注解递归
     * 2. 继承方向：❌ 不查父类方法 (根据你的要求，且 Spring 也不支持方法注解继承)
     */
    fun resolveForMethod(psiMethod: PsiMethod, targetAnnotations: Collection<String>): PsiAnnotation? {
        return resolveForElement(psiMethod, targetAnnotations)
    }

    /**
     * 通用基础查找：只负责在当前元素上找，以及递归找元注解
     */
    private fun resolveForElement(
        owner: PsiModifierListOwner,
        targetAnnotations: Collection<String>
    ): PsiAnnotation? {
        val visited = mutableSetOf<String>()

        // 遍历当前元素头顶上的所有注解
        for (annotation in owner.annotations) {
            val found = findRecursively(annotation, visited, targetAnnotations)
            if (found != null) {
                return found
            }
        }
        return null
    }

    /**
     * 递归检查 (Meta-Annotation Recursion)
     * 只要找到 targets 中的任意一个，立即返回该注解实例
     */
    private fun findRecursively(
        annotation: PsiAnnotation,
        visited: MutableSet<String>,
        targetAnnotations: Collection<String>
    ): PsiAnnotation? {
        val qName = annotation.qualifiedName ?: return null

        // 1. 命中检查 (Base Case)
        // 如果当前注解就是我们要找的目标之一，直接返回它
        if (targetAnnotations.contains(qName)) {
            return annotation
        }

        // 2. 防止死循环
        if (!visited.add(qName)) {
            return null
        }

        // 3. 性能优化：排除标准库注解，不进去浪费时间
        if (qName.startsWith("java.") || qName.startsWith("kotlin.") || qName.startsWith("javax.")) {
            return null
        }

        // 4. 解析注解定义 (Resolve to PsiClass)
        // 这里的 resolve() 会跳转到 @interface MyAnnotation 的定义处
        val annotationClass = annotation.nameReferenceElement?.resolve() as? PsiClass ?: return null

        // 5. 递归检查定义处的元注解
        for (metaAnnotation in annotationClass.annotations) {
            val found = findRecursively(metaAnnotation, visited, targetAnnotations)
            if (found != null) {
                // 注意：这里返回的是深层找到的那个目标注解 (Target)，而不是外层的 wrapper
                return found
            }
        }

        return null
    }

    /**
     * 入口方法：是否存在某个注解，对于类
     * 同时也负责处理【方向一：类继承】
     */
    fun hasAnnotations(psiClass: PsiClass, targetAnnotations: Collection<String>, findAll: Boolean = true): Boolean {
        // 1. 循环向上查找父类 (Class Inheritance)
        var currentClass: PsiClass? = psiClass
        while (currentClass != null && currentClass.qualifiedName != "java.lang.Object") {
            // 2. 检查当前这个类是否有目标注解 (包含元注解检查)
            if (hasTargetAnnotation(currentClass, targetAnnotations, findAll)) {
                return true
            }
            // 继续往上找父类
            currentClass = currentClass.superClass
        }
        return false
    }

    /**
     * 检查一个元素（类）上是否标记了 Controller 注解
     * 负责处理【方向二：元注解/注解继承】
     */
    private fun hasTargetAnnotation(
        owner: PsiModifierListOwner,
        targetAnnotations: Collection<String>,
        findAll: Boolean = true
    ): Boolean {
        // 遍历该类头上的所有注解
        for (annotation in owner.annotations) {
            // 使用递归检查这个注解是否是 RestController 或者 被 RestController 标记
            if (isMetaAnnotated(annotation, mutableSetOf(), targetAnnotations, findAll)) {
                return true
            }
        }
        return false
    }

    /**
     * 递归检查注解是否是目标注解 (Meta-Annotation Recursion)
     * @param visited 用于防止循环引用死循环 (比如 @A 注解了 @B, @B 又注解了 @A)
     */
    private fun isMetaAnnotated(
        annotation: PsiAnnotation,
        visited: MutableSet<String>,
        targetAnnotations: Collection<String>, // 建议用 List/Set，不用 MutableList，因为只读
        findAll: Boolean = true,
        // 🟢 新增：内部累加器，用于记录在递归路径上已经找到的注解
        // 默认值为空集合，外部调用时不需要传这个参数
        foundSoFar: MutableSet<String> = mutableSetOf()
    ): Boolean {
        val qName = annotation.qualifiedName ?: return false

        // 1. 命中检查：如果当前注解是目标之一
        if (targetAnnotations.contains(qName)) {
            foundSoFar.add(qName)
        }

        // 2. 核心判断逻辑 (Base Case)
        if (findAll) {
            // 模式 A：全量匹配
            // 如果已经收集齐了所有目标，立即返回 true
            if (foundSoFar.containsAll(targetAnnotations)) {
                return true
            }
        } else {
            // 模式 B：任意匹配
            // 只要发现任意一个目标 (累加器不为空)，立即返回 true
            if (foundSoFar.isNotEmpty()) {
                return true
            }
        }

        // 3. 防止死循环
        if (!visited.add(qName)) {
            return false
        }

        // 4. 性能优化：排除标准库注解
        if (qName.startsWith("java.") || qName.startsWith("kotlin.")) {
            return false
        }

        // 5. 解析注解定义
        // 使用 nameReferenceElement?.resolve() 获取类定义
        val annotationClass = annotation.nameReferenceElement?.resolve() as? PsiClass ?: return false

        // 6. 递归检查元注解
        for (metaAnnotation in annotationClass.annotations) {
            // 🟢 关键点：将 foundSoFar 传递给下一层
            // 因为 foundSoFar 是 MutableSet，引用传递，子递归查到的结果，兄弟递归也能看到
            if (isMetaAnnotated(metaAnnotation, visited, targetAnnotations, findAll, foundSoFar)) {
                return true
            }
        }

        // 7. 兜底检查 (针对 findAll = true 的情况)
        // 有可能在遍历完所有子节点后，凑齐了所有标签（例如分支A找到了标签1，分支B找到了标签2）
        // 所以这里再次检查一次
        if (findAll && foundSoFar.containsAll(targetAnnotations)) {
            return true
        }

        return false
    }
}