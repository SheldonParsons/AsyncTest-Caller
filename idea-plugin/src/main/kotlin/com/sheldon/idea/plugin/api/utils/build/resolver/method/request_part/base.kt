package com.sheldon.idea.plugin.api.utils.build.resolver.method.request_part

import com.intellij.openapi.module.Module
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiUtil
import com.sheldon.idea.plugin.api.method.AsyncTestType
import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.service.SpringClassName
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper

interface RequestPartResolver {
    fun push(variable: ParamAnalysisResult, apiRequest: ApiRequest, module: Module): ApiRequest

    /**
     * 辅助：判断是否为 Map 或纯 Object (这些不需要解析字段)
     */
    fun isGeneralObject(type: PsiType): Boolean {
        return type.canonicalText == SpringClassName.JAVA_BASE_OBJECT
    }

    fun isArrayOrCollection(type: PsiType): Boolean {
        // 1. 也是数组 (String[])
        if (type is PsiArrayType) {
            return true
        }

        // 2. 或是 Collection 接口本身 (兼容泛型 Collection<T>)
        if (type.canonicalText.startsWith(CommonClassNames.JAVA_UTIL_COLLECTION)) {
            return true
        }

        // 3. 或是继承自 Collection (List, Set, ArrayList...)
        // 注意：Map 不继承 Collection，所以 Map 会在这里返回 false (符合预期)
        return InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_COLLECTION)
    }

    /**
     * 核心逻辑：PSI Type -> AsyncTestType 字符串
     */
    fun mapToAsyncType(type: PsiType): String {
        val canonicalText = type.canonicalText

        // 1. 文件类型 (最高优)
        if (ResolverHelper.isMultipartFile(type)) {
            return AsyncTestType.FILES
        }

        // 2. 数组或集合 (解决继承 Collection 的问题)
        // 只要是数组，或者继承了 Collection，统统视为 array
        if (isArrayOrCollection(type)) {
            return AsyncTestType.ARRAY
        }

        // 3. 基础类型映射
        if (PsiTypes.intType().isAssignableFrom(type) || PsiTypes.longType()
                .isAssignableFrom(type) || PsiTypes.shortType().isAssignableFrom(type) || PsiTypes.byteType()
                .isAssignableFrom(type) || canonicalText == CommonClassNames.JAVA_LANG_INTEGER || canonicalText == CommonClassNames.JAVA_LANG_LONG
        ) {
            return AsyncTestType.INTEGER
        }

        if (PsiTypes.doubleType().isAssignableFrom(type) || PsiTypes.floatType()
                .isAssignableFrom(type) || canonicalText == SpringClassName.JAVA_MATH_BIG_DECIMAL
        ) {
            return AsyncTestType.NUMBER
        }

        if (PsiTypes.booleanType().isAssignableFrom(type) || canonicalText == CommonClassNames.JAVA_LANG_BOOLEAN) {
            return AsyncTestType.BOOLEAN
        }

        if (PsiTypes.voidType().isAssignableFrom(type)) {
            return AsyncTestType.NULL
        }

        // 4. 字符串 (包括 Date, Enum, CharSequence)
        // 注意：Enum 在 JSON 中通常也是 String
        if (canonicalText == CommonClassNames.JAVA_LANG_STRING || InheritanceUtil.isInheritor(
                type, CommonClassNames.JAVA_UTIL_DATE
            ) || InheritanceUtil.isInheritor(type, SpringClassName.JAVA_TIME_TEMPORAL) || // LocalDate 等
            PsiUtil.resolveClassInType(type)?.isEnum == true
        ) {
            return AsyncTestType.STRING
        }

        if (isMapType(type)) {
            return AsyncTestType.OBJECT
        }

        // 5. 兜底：其余的都是 Object (Map 也算 Object)
        return AsyncTestType.DS
    }

    fun isMapType(type: PsiType): Boolean {
        // 1. 先判断全限定名是否直接相等 (针对 type 就是 Map 接口本身的情况)
        if (type.canonicalText.startsWith(CommonClassNames.JAVA_UTIL_MAP)) {
            // 注意：这里用 startsWith 是为了兼容泛型，比如 java.util.Map<K,V>
            return true
        }

        // 2. 再判断继承关系 (针对 HashMap, TreeMap 等)
        return InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_MAP)
    }


    fun extractMapValueType(type: PsiType): PsiType? {
        val resolveResult = PsiUtil.resolveGenericsClassInType(type)
        val psiClass = resolveResult.element ?: return null

        // Map 接口通常有两个泛型参数 <K, V>
        // 我们通过 typeParameters 获取参数定义的数组
        val typeParams = psiClass.typeParameters
        if (typeParams.size == 2) {
            return resolveResult.substitutor.substitute(typeParams[1])
        }

        return null
    }

    /**
     * 辅助：提取数组/集合的元素类型
     */
    fun extractArrayComponentType(type: PsiType): PsiType? {
        // 情况 A: 数组 String[]
        if (type is PsiArrayType) {
            return type.componentType
        }

        // 情况 B: 集合 List<String>
        val resolveResult = PsiUtil.resolveGenericsClassInType(type)
        resolveResult.element ?: return null // 如果解析不出类（比如是 int），直接返回 null
        // 尝试获取泛型参数
        return resolveResult.substitutor.substitutionMap.values.firstOrNull()
    }

}