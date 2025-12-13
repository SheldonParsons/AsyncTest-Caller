package com.sheldon.idea.plugin.api.utils.build.resolver.method.parameter


import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil
import com.sheldon.idea.plugin.api.method.ParamLocation
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult

class SimpleTypeResolver : MethodParameterResolver {

    override fun resolve(parameter: PsiParameter, method: PsiMethod, psiClass: PsiClass): ParamAnalysisResult? {
        // 核心判断：是否为简单类型（或简单类型的集合）
        if (isSimpleType(parameter.type)) {
            return ParamAnalysisResult(
                location = ParamLocation.QUERY, // 隐式简单类型 -> Query 参数
                name = parameter.name,          // 直接使用参数名
                t = parameter.type, isRequired = true // Spring 对非包装的基本类型(int)默认必填，对象类型(Integer)默认非必填。
            )
        }
        return null
    }

    /**
     * 判断是否为 "简单类型" (Spring MVC 可以直接通过 Converter 转换的类型)
     * 包括：基本类型、String、Date、Number、Enum、URI、URL、UUID、Locale...
     * 以及：上述类型的数组或集合 (例如 List<String>, int[])
     */
    fun isSimpleType(type: PsiType): Boolean {
        // 1. 基本类型 (int, double, boolean...)
        if (type is PsiPrimitiveType) return true

        // 2. 数组类型 (String[], int[]) -> 递归检查元素类型
        if (type is PsiArrayType) {
            return isSimpleType(type.componentType)
        }

        // 3. 解析类定义
        val psiClass = PsiUtil.resolveClassInType(type) ?: return false
        val qName = psiClass.qualifiedName ?: return false

        // 4. 集合类型 (List<String>) -> 递归检查泛型元素类型
        if (isCollection(qName)) {
            val itemType = PsiUtil.extractIterableTypeParameter(type, false)
            return if (itemType != null) {
                isSimpleType(itemType)
            } else {
                true
            }
        }

        // 5. 枚举 (Enum) -> 肯定是简单类型
        if (psiClass.isEnum) return true

        // 6. 白名单匹配 (最核心的部分)
        return isWhitelistedSimpleType(qName)
    }

    /**
     * 简单类型白名单
     */
    private fun isWhitelistedSimpleType(qName: String): Boolean {
        return when {
            // 基础文本
            qName == "java.lang.String" -> true
            qName == "java.lang.CharSequence" -> true

            // 包装类 & 数字 (Integer, Double, BigDecimal...)
            qName.startsWith("java.lang.") && isWrapperOrNumber(qName) -> true
            qName.startsWith("java.math.") -> true // BigDecimal, BigInteger

            // 日期时间 (Legacy + Java 8)
            qName == "java.util.Date" -> true
            qName == "java.sql.Date" -> true
            qName == "java.sql.Timestamp" -> true
            qName.startsWith("java.time.") -> true // LocalDate, LocalDateTime, ZonedDateTime, Duration...

            // 常用工具
            qName == "java.util.UUID" -> true
            qName == "java.util.Currency" -> true
            qName == "java.util.Locale" -> true
            qName == "java.net.URI" -> true
            qName == "java.net.URL" -> true
            qName == "java.nio.charset.Charset" -> true

            else -> false
        }
    }

    private fun isWrapperOrNumber(qName: String): Boolean {
        return qName == "java.lang.Boolean" || qName == "java.lang.Character" || qName == "java.lang.Byte" || qName == "java.lang.Short" || qName == "java.lang.Integer" || qName == "java.lang.Long" || qName == "java.lang.Float" || qName == "java.lang.Double" || qName == "java.lang.Number"
    }

    private fun isCollection(qName: String): Boolean {
        // 简单判断常见集合接口
        return qName == "java.util.Collection" || qName == "java.util.List" || qName == "java.util.Set" || qName == "java.util.ArrayList" || qName == "java.util.LinkedList" || qName == "java.util.HashSet"
    }
}