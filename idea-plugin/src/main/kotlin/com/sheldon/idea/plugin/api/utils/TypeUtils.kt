package com.sheldon.idea.plugin.api.utils

import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.util.TypeConversionUtil
import com.sheldon.idea.plugin.api.method.AsyncTestType
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper

object TypeUtils {

    fun mapToAsyncTestType(type: PsiType?): String {
        if (type == null) return AsyncTestType.STRING

        // 1. 数组类型
        if (type is PsiArrayType || ResolverHelper.isInheritor(type, "java.util.Collection")) {
            return AsyncTestType.ARRAY
        }

        // 2. 数字类型
        if (type == PsiTypes.intType() || type == PsiTypes.longType() || type == PsiTypes.shortType() || ResolverHelper.isInheritor(
                type,
                "java.lang.Integer"
            ) || ResolverHelper.isInheritor(type, "java.lang.Long")
        ) {
            return AsyncTestType.INTEGER
        }

        if (type == PsiTypes.floatType() || type == PsiTypes.doubleType() || ResolverHelper.isInheritor(
                type,
                "java.lang.Double"
            ) || ResolverHelper.isInheritor(type, "java.lang.Float") || ResolverHelper.isInheritor(
                type,
                "java.math.BigDecimal"
            )
        ) {
            return AsyncTestType.NUMBER
        }

        // 3. 布尔类型
        if (type == PsiTypes.booleanType() || ResolverHelper.isInheritor(type, "java.lang.Boolean")) {
            return AsyncTestType.BOOLEAN
        }

        // 4. 文件类型 (Body 用)
        if (ResolverHelper.isInheritor(
                type,
                "org.springframework.web.multipart.MultipartFile"
            ) || ResolverHelper.isInheritor(type, "javax.servlet.http.Part") || ResolverHelper.isInheritor(
                type,
                "jakarta.servlet.http.Part"
            )
        ) {
            return AsyncTestType.FILES
        }

        // 默认为 string (包括 String, Date, Enum, UUID 等)
        return AsyncTestType.STRING
    }

    // 兼容泛型的获取真实类型
    fun getRealTypeForMethod(method: PsiMethod, type: PsiType, currentClass: PsiClass): PsiType {
        val containingClass = method.containingClass

        // 2. 如果方法是在父类定义的，且当前类不是定义类，就需要进行泛型推断
        val realType = if (containingClass != null && containingClass != currentClass) {
            val substitutor = TypeConversionUtil.getSuperClassSubstitutor(
                containingClass, currentClass, PsiSubstitutor.EMPTY
            )
            substitutor.substitute(type)
        } else {
            type
        }
        return realType
    }
}