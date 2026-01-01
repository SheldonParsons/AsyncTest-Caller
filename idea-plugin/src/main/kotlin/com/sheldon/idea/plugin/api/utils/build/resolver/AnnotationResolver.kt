package com.sheldon.idea.plugin.api.utils.build.resolver
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiBinaryExpression
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.impl.JavaConstantExpressionEvaluator
import com.sheldon.idea.plugin.api.service.SpringClassName
object AnnotationResolver {
    fun getAnnotationAttribute(annotation: PsiAnnotation, attrName: String): PsiAnnotationMemberValue? {
        return annotation.findAttributeValue(attrName)
    }
    fun getAnnotationAttributeValues(annotation: PsiAnnotation, attrName: String): List<String> {
        val value = annotation.findAttributeValue(attrName) ?: return emptyList()
        val result = mutableListOf<String>()
        fun extract(element: PsiElement) {
            when (element) {
                is PsiLiteralExpression -> {
                    val text = element.value as? String
                    if (text != null) result.add(text)
                }
                is PsiReferenceExpression -> {
                    val resolve = element.resolve()
                    if (resolve is PsiField) {
                        val constantVal = resolve.computeConstantValue() as? String
                        if (constantVal != null) result.add(constantVal)
                    }
                }
                is PsiBinaryExpression -> {
                    try {
                        val computed = JavaConstantExpressionEvaluator.computeConstantExpression(element, false)
                        if (computed != null && computed is String) {
                            result.add(computed)
                        }
                    } catch (e: Exception) {
                    }
                }
                is PsiExpression -> {
                    val computed = JavaConstantExpressionEvaluator.computeConstantExpression(element, false)
                    if (computed is String) result.add(computed)
                }
            }
        }
        if (value is PsiArrayInitializerMemberValue) {
            value.initializers.forEach { extract(it) }
        } else {
            extract(value)
        }
        return result
    }
    fun getPath(annotation: PsiAnnotation): String {
        return this.getAnnotationAttributeValues(annotation, SpringClassName.ATTR_VALUE)
            .ifEmpty { this.getAnnotationAttributeValues(annotation, SpringClassName.ATTR_PATH) }
            .firstOrNull() ?: ""
    }
    fun <T> parseConsumes(
        annotation: PsiAnnotation,
        attributeName: String,
        once: Boolean = true,
        factory: (value: String) -> T
    ) {
        val rawStrings = this.getAnnotationAttributeValues(annotation, attributeName)
        for (raw in rawStrings) {
            factory(raw)
            if (once) break
        }
    }
    fun <T> parseParamsOrHeaders(
        annotation: PsiAnnotation,
        attributeName: String,
        factory: (key: String, value: String) -> T
    ): MutableList<T> {
        val result = mutableListOf<T>()
        val rawStrings = this.getAnnotationAttributeValues(annotation, attributeName)
        for (raw in rawStrings) {
            val constraint = raw.trim()
            if (constraint.isEmpty()) continue
            if (constraint.startsWith("!")) {
                if (constraint.contains("=")) {
                    val parts = constraint.split("=", limit = 2)
                    val key = parts[0].trim().removePrefix("!")
                    val originalValue = parts.getOrElse(1) { "" }.trim()
                    val fakeValue = generateNotEqualValue(originalValue)
                    result.add(factory(key, fakeValue))
                    continue
                } else {
                    continue
                }
            }
            if (constraint.contains("=")) {
                val parts = constraint.split("=", limit = 2)
                val key = parts[0].trim()
                val value = parts.getOrElse(1) { "" }.trim()
                result.add(factory(key, value))
                continue
            }
            result.add(factory(constraint, ""))
        }
        return result
    }
    /**
     * 通用基础查找：只负责在当前元素上找，以及递归找元注解
     */
    private fun resolveForElement(
        owner: PsiModifierListOwner,
        targetAnnotations: Collection<String>
    ): PsiAnnotation? {
        val visited = mutableSetOf<String>()
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
        if (targetAnnotations.contains(qName)) {
            return annotation
        }
        if (!visited.add(qName)) {
            return null
        }
        if (qName.startsWith("java.") || qName.startsWith("kotlin.") || qName.startsWith("javax.")) {
            return null
        }
        val annotationClass = annotation.nameReferenceElement?.resolve() as? PsiClass ?: return null
        for (metaAnnotation in annotationClass.annotations) {
            val found = findRecursively(metaAnnotation, visited, targetAnnotations)
            if (found != null) {
                return found
            }
        }
        return null
    }
    /**
     * 是否存在某个注解，对于类
     */
    fun hasAnnotations(psiClass: PsiClass, targetAnnotations: Collection<String>, findAll: Boolean = true): Boolean {
        var currentClass: PsiClass? = psiClass
        while (currentClass != null && currentClass.qualifiedName != "java.lang.Object") {
            if (hasTargetAnnotation(currentClass, targetAnnotations, findAll)) {
                return true
            }
            currentClass = currentClass.superClass
        }
        return false
    }
    /**
     * 不仅查当前类，还会去查实现的接口、继承的父类
     */
    fun findAnnotationInHierarchy(psiClassOrMethod: PsiModifierListOwner?, fqn: String): PsiAnnotation? {
        if (psiClassOrMethod == null) return null
        // 方案 A: 使用 IntelliJ SDK 原生工具类 (推荐，最稳健)
        // 它会自动处理类继承、接口实现，甚至处理复杂的层级关系
        return AnnotationUtil.findAnnotationInHierarchy(psiClassOrMethod, setOf(fqn))
    }
    /**
     * 检查一个元素（类）上是否标记了 Controller 注解
     */
    private fun hasTargetAnnotation(
        owner: PsiModifierListOwner,
        targetAnnotations: Collection<String>,
        findAll: Boolean = true
    ): Boolean {
        for (annotation in owner.annotations) {
            if (isMetaAnnotated(annotation, mutableSetOf(), targetAnnotations, findAll)) {
                return true
            }
        }
        return false
    }
    /**
     * 递归查找带有 Spring Mapping 注解的方法
     * 逻辑：先查自己 -> 没有则查父类/接口 (DFS/BFS)
     */
    fun findMethodWithMapping(method: PsiMethod): PsiMethod? {
        if (hasSpringMapping(method)) {
            return method
        }
        for (superMethod in method.findSuperMethods()) {
            val found = findMethodWithMapping(superMethod)
            if (found != null) {
                return found
            }
        }
        return null
    }
    fun isMappingMethod(method: PsiMethod): Boolean {
        if (hasSpringMapping(method)) return true
        for (superMethod in method.findSuperMethods(true)) {
            if (hasSpringMapping(superMethod)) return true
        }
        return false
    }
    /**
     * 辅助方法：判断某个方法上是否有 Spring Mapping 注解
     */
    private fun hasSpringMapping(method: PsiMethod): Boolean {
        for (annoName in SpringClassName.SPRING_SINGLE_REQUEST_MAPPING_ANNOTATIONS) {
            if (AnnotationUtil.isAnnotated(method, annoName, 0)) {
                return true
            }
        }
        return false
    }
    /**
     * 递归检查注解是否是目标注解 (Meta-Annotation Recursion)
     */
    private fun isMetaAnnotated(
        annotation: PsiAnnotation,
        visited: MutableSet<String>,
        targetAnnotations: Collection<String>,
        findAll: Boolean = true,
        foundSoFar: MutableSet<String> = mutableSetOf()
    ): Boolean {
        val qName = annotation.qualifiedName ?: return false
        if (targetAnnotations.contains(qName)) {
            foundSoFar.add(qName)
        }
        if (findAll) {
            if (foundSoFar.containsAll(targetAnnotations)) {
                return true
            }
        } else {
            if (foundSoFar.isNotEmpty()) {
                return true
            }
        }
        if (!visited.add(qName)) {
            return false
        }
        if (qName.startsWith("java.") || qName.startsWith("kotlin.")) {
            return false
        }
        val annotationClass = annotation.nameReferenceElement?.resolve() as? PsiClass ?: return false
        for (metaAnnotation in annotationClass.annotations) {
            if (isMetaAnnotated(metaAnnotation, visited, targetAnnotations, findAll, foundSoFar)) {
                return true
            }
        }
        if (findAll && foundSoFar.containsAll(targetAnnotations)) {
            return true
        }
        return false
    }
    private fun generateNotEqualValue(original: String): String {
        if (original.isEmpty()) return "not_empty"
        val chars = original.toCharArray()
        val lastIndex = chars.lastIndex
        val lastChar = chars[lastIndex]
        chars[lastIndex] = if (lastChar.code > 33) {
            (lastChar.code - 1).toChar()
        } else {
            (lastChar.code + 1).toChar()
        }
        return String(chars)
    }
}
fun PsiParameter.findAnnotationInHierarchy(annotationFqn: String): PsiAnnotation? {
    // 1. 先查自己 (最快)
    val selfAnnotation = this.getAnnotation(annotationFqn)
    if (selfAnnotation != null) {
        return selfAnnotation
    }
    // 2. 获取当前参数所属的方法 (Scope)
    val method = this.declarationScope as? PsiMethod ?: return null
    // 3. 计算自己是第几个参数 (Index)
    // index 会被缓存，性能消耗极小
    val index = method.parameterList.getParameterIndex(this)
    if (index == -1) return null
    // 4. 查找所有父类/接口方法
    val superMethods = method.findSuperMethods()
    for (superMethod in superMethods) {
        val superParams = superMethod.parameterList.parameters
        // 安全检查：防止父类方法参数列表长度不一致（极少见）
        if (index < superParams.size) {
            val superParam = superParams[index]
            val superAnnotation = superParam.getAnnotation(annotationFqn)
            if (superAnnotation != null) {
                return superAnnotation
            }
        }
    }
    return null
}