package com.sheldon.idea.plugin.api.utils.build.resolver

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiBinaryExpression
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.JavaConstantExpressionEvaluator
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiUtil
import com.sheldon.idea.plugin.api.service.SpringClassName

class ResolverHelper {

    companion object {
        fun getAnnotationAttributeValues(annotation: PsiAnnotation, attrName: String): List<String> {
            val value = annotation.findAttributeValue(attrName) ?: return emptyList()
            val result = mutableListOf<String>()

            fun extract(element: PsiElement) {
                when (element) {
                    // 1. 字面量 ("/api")
                    is PsiLiteralExpression -> {
                        val text = element.value as? String
                        if (text != null) result.add(text)
                    }

                    // 2. 引用 (MyConst.URL)
                    // 注意：其实 JavaConstantExpressionEvaluator 也能处理引用
                    // 这里保留手动 resolve 是为了容错，或者你可以把这个分支也合并到下面去
                    is PsiReferenceExpression -> {
                        val resolve = element.resolve()
                        if (resolve is PsiField) {
                            val constantVal = resolve.computeConstantValue() as? String
                            if (constantVal != null) result.add(constantVal)
                        }
                    }

                    // 3. 拼接表达式 ("A" + "B" 或 Const.A + "/b")
                    is PsiBinaryExpression -> {
                        // 使用 IDEA 原生计算器，它极强！
                        try {
                            val computed = JavaConstantExpressionEvaluator.computeConstantExpression(element, false)
                            if (computed != null && computed is String) {
                                result.add(computed)
                            }
                        } catch (e: Exception) {
                            // 忽略计算错误
                        }
                    }

                    // 4. 也是一种更通用的表达式处理 (比如括号表达式 ("a" + "b"))
                    is PsiExpression -> {
                        // 兜底：尝试对任何表达式求值
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

        fun parseRequestMethod(annotation: PsiAnnotation): SpringClassName.RequestMethod? {
            val qName = annotation.qualifiedName ?: return null

            // 1. 如果是 @GetMapping 等具体注解 -> 直接查表
            if (SpringClassName.METHOD_ANNOTATION_MAP.containsKey(qName)) {
                val method = SpringClassName.METHOD_ANNOTATION_MAP[qName]!!
                return listOf(method).firstOrNull()
            }

            // 2. 如果是 @RequestMapping -> 解析 method 属性
            if (qName == SpringClassName.REQUEST_MAPPING_ANNOTATION) {
                val valueElement = annotation.findAttributeValue(SpringClassName.ATTR_METHOD)
                    ?: return null
                return resolveMethodEnumValues(valueElement)
            }

            return null
        }

        fun isMultipartFile(type: PsiType): Boolean {
            if (isSingleFile(type)) return true

            if (type is com.intellij.psi.PsiArrayType) {
                // 剥开数组看元素
                val itemType = type.componentType
                return isSingleFile(itemType)
            }

            val itemType = PsiUtil.extractIterableTypeParameter(type, false)
            if (itemType != null) {
                return isSingleFile(itemType)
            }

            return false
        }

        private fun isSingleFile(type: PsiType): Boolean {
            return ResolverHelper.isInheritor(type, SpringClassName.MULTI_PART_FILE) || ResolverHelper.isInheritor(
                type,
                SpringClassName.JAVAX_PART
            ) || ResolverHelper.isInheritor(type, SpringClassName.JAKARTA_PART)
        }

        /**
         * 判断 type 是否是 targetFqn 的子类或实现类 (包含自身)
         * 例如: isInheritor(type, "java.util.List")
         */
        fun isInheritor(type: PsiType, targetFqn: String): Boolean {
            // 1. 解析出 PsiClass
            val psiClass = PsiUtil.resolveClassInType(type) ?: return false

            // 2. 快速路径：检查自身名字是否完全一致
            // (注意：InheritanceUtil.isInheritor 有时只查父类，不包含自身，所以这步必须有)
            if (psiClass.qualifiedName == targetFqn) {
                return true
            }

            // 3. 查家谱：检查是否继承了目标类
            return InheritanceUtil.isInheritor(psiClass, targetFqn)
        }

        private fun resolveMethodEnumValues(element: PsiElement): SpringClassName.RequestMethod? {
            val methods = mutableListOf<SpringClassName.RequestMethod>()

            fun extract(item: PsiElement) {
                // item 是源代码中的元素，比如: RequestMethod.POST
                if (item is PsiReferenceExpression) {
                    // item.referenceName 就是引用的名字，即 "POST"
                    val name = item.referenceName

                    // 将字符串 "POST" 转为你自己的枚举
                    val methodEnum = SpringClassName.RequestMethod.from(name)
                    if (methodEnum != null) {
                        methods.add(methodEnum)
                    }
                }
            }

            if (element is PsiArrayInitializerMemberValue) {
                element.initializers.forEach { extract(it) }
            } else {
                extract(element)
            }

            return methods.firstOrNull()
        }

        fun getPath(annotation: PsiAnnotation): String {
            return this.getAnnotationAttributeValues(annotation, SpringClassName.ATTR_VALUE)
                .ifEmpty { this.getAnnotationAttributeValues(annotation, SpringClassName.ATTR_PATH) }
                .firstOrNull() ?: ""
        }

        /**
         * 通用解析入口
         * @param annotation 注解对象
         * @param attributeName 属性名 (SpringConstants.ATTR_PARAMS 或 ATTR_HEADERS)
         * @param factory 工厂函数，用于生成目标对象 (比如生成 Header 或 Query)
         */
        fun <T> parseConsumes(
            annotation: PsiAnnotation,
            attributeName: String,
            once: Boolean = true,
            factory: (value: String) -> T
        ) {
            // 比如: {"application/json", "application/xml"}
            val rawStrings = this.getAnnotationAttributeValues(annotation, attributeName)
            for (raw in rawStrings) {
                factory(raw)
                if (once) break
            }
        }


        /**
         * 通用解析入口
         * @param annotation 注解对象
         * @param attributeName 属性名 (SpringConstants.ATTR_PARAMS 或 ATTR_HEADERS)
         * @param factory 工厂函数，用于生成目标对象 (比如生成 Header 或 Query)
         */
        fun <T> parseParamsOrHeaders(
            annotation: PsiAnnotation,
            attributeName: String,
            factory: (key: String, value: String) -> T
        ): MutableList<T> {
            val result = mutableListOf<T>()

            // 1. 利用之前封装的工具，将 PsiElement (字面量/常量/数组) 解析为字符串列表
            // 比如: {"a", "b=123", "!c", "!d=456"}
            val rawStrings = this.getAnnotationAttributeValues(annotation, attributeName)

            // 2. 遍历解析语法
            for (raw in rawStrings) {
                val constraint = raw.trim()
                if (constraint.isEmpty()) continue

                // 规则 3a: "!a"、"!a=123"
                if (constraint.startsWith("!")) {
                    if (constraint.contains("=")) {
                        val parts = constraint.split("=", limit = 2)
                        val key = parts[0].trim()
                        val originalValue = parts.getOrElse(1) { "" }.trim()

                        // 生成一个不等于原值的新值 (保持长度一致)
                        val fakeValue = generateNotEqualValue(originalValue)

                        result.add(factory(key, fakeValue))
                        continue
                    } else {
                        continue
                    }
                }

                // 规则 3b & 5: "a=123" (等于)
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
         * 生成不等于原值的模拟值
         * 规则：长度一致，修改最后一位字符
         */
        private fun generateNotEqualValue(original: String): String {
            if (original.isEmpty()) return "not_empty"

            val chars = original.toCharArray()
            val lastIndex = chars.lastIndex
            val lastChar = chars[lastIndex]

            // 简单的字符翻转算法
            chars[lastIndex] = if (lastChar.code > 33) {
                (lastChar.code - 1).toChar()
            } else {
                (lastChar.code + 1).toChar()
            }

            return String(chars)
        }

        /**
         * 合并两个列表，后者覆盖前者
         *
         * @param T 列表元素的类型 (比如 Header 或 Query)
         * @param baseList 基础列表 (优先级低)
         * @param overlayList 覆盖列表 (优先级高)
         * @param keySelector 告诉函数哪个属性是 "Key" (比如 it.name)
         */
        fun <T> mergeHeadersOrParams(
            baseList: MutableList<T>?,
            overlayList: MutableList<T>?,
            distinct: Boolean = true, // 默认去重
            keySelector: ((T) -> String)? = null // 放在最后，且可空
        ): MutableList<T> {
            // 0. 准备结果容器
            val result = mutableListOf<T>() // ✅ 改用 mutableListOf

            // 1. 如果不需要去重，直接拼接
            if (!distinct) {
                if (baseList != null) result.addAll(baseList)
                if (overlayList != null) result.addAll(overlayList)
                return result
            }

            // 2. 如果需要去重，但没有提供 keySelector，则使用对象本身的 equals/hashCode 去重
            if (keySelector == null) {
                val set = LinkedHashSet<T>()
                if (baseList != null) set.addAll(baseList)
                if (overlayList != null) set.addAll(overlayList)
                result.addAll(set)
                return result
            }

            // 3. 按 Key 去重 (你的核心逻辑)
            // 使用 LinkedHashMap 保持插入顺序
            val map = LinkedHashMap<String, T>()

            baseList?.forEach { item ->
                val key = keySelector(item)
                if (key.isNotEmpty()) map[key] = item
            }

            overlayList?.forEach { item ->
                val key = keySelector(item)
                if (key.isNotEmpty()) map[key] = item
            }

            result.addAll(map.values)
            return result
        }

        /**
         * 向列表添加或更新元素
         * @param list 目标列表 (会被修改)
         * @param element 要添加的元素
         * @param keySelector 获取 Key 的逻辑
         * @param overwrite true: 如果 Key 存在则覆盖 (去重); false: 直接追加 (不去重)
         */
        fun <T> addOrUpdateElement(
            list: MutableList<T>,
            element: T,
            overwrite: Boolean = true, // 默认覆盖
            keySelector: ((T) -> String)? = null // 放在最后，且可空
        ) {
            // 1. 如果不需要覆盖（允许重复），直接追加
            if (!overwrite) {
                list.add(element)
                return
            }

            // 2. 需要覆盖
            var index = -1

            if (keySelector != null) {
                // 2a. 如果提供了 Key 选择器，按 Key 查找
                val key = keySelector(element)
                if (key.isNotEmpty()) {
                    index = list.indexOfFirst { keySelector(it) == key }
                }
            } else {
                // 2b. 如果没提供 Key 选择器，按对象相等性查找
                index = list.indexOf(element)
            }

            if (index != -1) {
                // 存在 -> 原地替换
                list[index] = element
            } else {
                // 不存在 -> 追加
                list.add(element)
            }
        }

        /**
         * 拼接 URL 路径
         * 规则：
         * 1. "" + "" -> ""
         * 2. "" + "a" -> "/a"
         * 3. "a" + "b" -> "/a/b" (自动补齐中间的 /)
         * 4. "a/" + "/b" -> "/a/b" (去重中间的 /)
         * 5. 结果总是以 / 开头 (除非结果为空)
         */
        fun combinePath(base: String?, sub: String?): String {
            // 0. 安全判空处理，将 null 视为 ""
            val safeBase = base?.trim() ?: ""
            val safeSub = sub?.trim() ?: ""

            // 规则 1: 空字符串和空字符串拼接，还是空字符串
            if (safeBase.isEmpty() && safeSub.isEmpty()) {
                return ""
            }

            // 核心逻辑：
            // 1. 去掉 base 尾部的 /
            val cleanBase = safeBase.removeSuffix("/")
            // 2. 去掉 sub 头部的 /
            val cleanSub = safeSub.removePrefix("/")

            // 3. 拼接 (中间补 /)
            // 规则 2 & 3: 处理单侧为空和双侧不为空的情况
            val combined = if (cleanBase.isEmpty()) {
                cleanSub
            } else if (cleanSub.isEmpty()) {
                cleanBase
            } else {
                "$cleanBase/$cleanSub"
            }

            // 规则 4: 拼接结束后，如果前面缺少/，需要补充/
            return if (!combined.startsWith("/")) {
                "/$combined"
            } else {
                combined
            }
        }

        /**
         * 获取类或字段的注释信息
         * 兼容 /** ... */ 和 // ...
         */
        fun getElementComment(element: PsiElement?): String {
            if (element == null) return ""

            val comments = mutableListOf<String>()

            // =================================================================
            // 策略 1: 尝试获取 Javadoc (标准 /** ... */)
            // =================================================================
            if (element is PsiDocCommentOwner && element.docComment != null) {
                return element.docComment!!.descriptionElements.joinToString("") { it.text }
                    .trim().replace(Regex("\\n\\s*\\*"), "\n").trim()
            }

            // =================================================================
            // 策略 2: 往里找 (Children) - 针对你遇到的情况
            // =================================================================
            // 有些 PSI 解析会将行注释归纳为 Field 的第一个子节点
            var child = element.firstChild
            while (child != null) {
                if (child is PsiComment) {
                    comments.add(cleanComment(child.text))
                } else if (child is PsiWhiteSpace) {
                    // 忽略空白，继续找
                } else {
                    // 一旦遇到非注释、非空白的东西（比如 public, static, 或类型定义），说明前面的注释找完了
                    break
                }
                child = child.nextSibling
            }

            // 如果在内部找到了注释，直接返回，不需要再往外找了
            if (comments.isNotEmpty()) {
                return comments.joinToString("\n")
            }

            // =================================================================
            // 策略 3: 往外找 (PrevSibling) - 针对标准情况
            // =================================================================
            var prev = element.prevSibling
            var safetyCount = 0
            while (prev != null && safetyCount < 20) {
                if (prev is PsiComment) {
                    comments.add(0, cleanComment(prev.text)) // 倒序插入
                } else if (prev is PsiWhiteSpace) {
                    // 如果连续换行超过 1 个，说明可能不属于当前字段
                    if (prev.text.count { it == '\n' } > 1) {
                        break
                    }
                } else if (prev is PsiAnnotation) {
                    // 跳过注解，继续往上找
                } else {
                    // 遇到其他代码，停止
                    break
                }
                prev = prev.prevSibling
                safetyCount++
            }

            return comments.joinToString("\n")
        }


        fun cleanComment(text: String): String {
            return text.trim()
                .replace(Regex("^//\\s*"), "")      // 去除开头的 //
                .replace(Regex("^/\\*+\\s*"), "")   // 去除开头的 /*
                .replace(Regex("\\s*\\*+/$"), "")   // 去除结尾的 */
                .trim()
        }

        fun debugPsiStructure(element: PsiElement) {
            println("\n🛑 DEBUG START: 正在检查字段 [${element.text.substringBefore("\n").take(20)}...]")

            var prev = element.prevSibling
            var count = 0

            while (prev != null && count < 10) {
                // 获取节点的具体类名 (比如 PsiCommentImpl, PsiWhiteSpaceImpl)
                val className = prev::class.java.simpleName
                // 获取节点文本 (把换行符显示出来，方便观察)
                val content = prev.text.replace("\n", "\\n").replace("\r", "\\r")

                println("   Previous[$count] -> 类型: $className | 内容: '$content'")

                prev = prev.prevSibling
                count++
            }
            println("🛑 DEBUG END\n")
        }
    }

}