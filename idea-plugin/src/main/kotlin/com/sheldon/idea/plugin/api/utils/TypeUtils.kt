package com.sheldon.idea.plugin.api.utils

import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.TypeConversionUtil
import com.sheldon.idea.plugin.api.method.AsyncTestType
import com.sheldon.idea.plugin.api.service.SpringClassName
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper

object TypeUtils {
    fun mapToAsyncType(type: PsiType): String {
        val canonicalText = type.canonicalText
        if (ResolverHelper.isMultipartFile(type)) {
            return AsyncTestType.FILES
        }
        if (isArrayOrCollection(type)) {
            return AsyncTestType.ARRAY
        }
        if (PsiType.INT.isAssignableFrom(type) || PsiType.LONG
                .isAssignableFrom(type) || PsiType.SHORT.isAssignableFrom(type) || PsiType.BYTE
                .isAssignableFrom(type) || canonicalText == CommonClassNames.JAVA_LANG_INTEGER || canonicalText == CommonClassNames.JAVA_LANG_LONG
        ) {
            return AsyncTestType.INTEGER
        }
        if (PsiType.DOUBLE.isAssignableFrom(type) || PsiType.FLOAT
                .isAssignableFrom(type) || canonicalText == SpringClassName.JAVA_MATH_BIG_DECIMAL
        ) {
            return AsyncTestType.NUMBER
        }
        if (PsiType.BOOLEAN.isAssignableFrom(type) || canonicalText == CommonClassNames.JAVA_LANG_BOOLEAN) {
            return AsyncTestType.BOOLEAN
        }
        if (PsiType.VOID.isAssignableFrom(type)) {
            return AsyncTestType.NULL
        }
        if (canonicalText == CommonClassNames.JAVA_LANG_STRING || InheritanceUtil.isInheritor(
                type, CommonClassNames.JAVA_UTIL_DATE
            ) || InheritanceUtil.isInheritor(type, SpringClassName.JAVA_TIME_TEMPORAL) ||
            PsiUtil.resolveClassInType(type)?.isEnum == true
        ) {
            return AsyncTestType.STRING
        }
        if (isMapType(type)) {
            return AsyncTestType.OBJECT
        }
        return AsyncTestType.DS
    }

    fun isArrayOrCollection(type: PsiType): Boolean {
        if (type is PsiArrayType) {
            return true
        }
        if (type.canonicalText.startsWith(CommonClassNames.JAVA_UTIL_COLLECTION)) {
            return true
        }
        return InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_COLLECTION)
    }

    fun isMapType(type: PsiType): Boolean {
        if (type.canonicalText.startsWith(CommonClassNames.JAVA_UTIL_MAP)) {
            return true
        }
        return InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_MAP)
    }

    fun isGeneralObject(type: PsiType): Boolean {
        return type.canonicalText == SpringClassName.JAVA_BASE_OBJECT
    }

    fun getRealTypeForMethod(method: PsiMethod, type: PsiType, currentClass: PsiClass): PsiType {
        val containingClass = method.containingClass
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