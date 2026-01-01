package com.sheldon.idea.plugin.api.utils.build.resolver.method.parameter
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil
import com.sheldon.idea.plugin.api.method.ParamLocation
import com.sheldon.idea.plugin.api.model.CodeType
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.docs.DocInfo
import com.sheldon.idea.plugin.api.utils.build.docs.DocResolver
class SimpleTypeResolver : MethodParameterResolver {
    override fun resolve(
        parameter: PsiParameter,
        method: PsiMethod,
        psiClass: PsiClass,
        implicitParams: MutableMap<String, DocInfo>,
        hasDocs: Boolean
    ): ParamAnalysisResult? {
        if (isSimpleType(parameter.type)) {
            val (docInfo, _) = DocResolver().resolve(parameter, mutableMapOf(), CodeType.PARAM, hasDocs)
            return ParamAnalysisResult(
                location = ParamLocation.QUERY,
                name = parameter.name,
                t = parameter.type, isRequired = true, docInfo = docInfo
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
        if (type is PsiPrimitiveType) return true
        if (type is PsiArrayType) {
            return isSimpleType(type.componentType)
        }
        val psiClass = PsiUtil.resolveClassInType(type) ?: return false
        val qName = psiClass.qualifiedName ?: return false
        if (isCollection(qName)) {
            val itemType = PsiUtil.extractIterableTypeParameter(type, false)
            return if (itemType != null) {
                isSimpleType(itemType)
            } else {
                true
            }
        }
        if (psiClass.isEnum) return true
        return isWhitelistedSimpleType(qName)
    }
    /**
     * 简单类型白名单
     */
    private fun isWhitelistedSimpleType(qName: String): Boolean {
        return when {
            qName == "java.lang.String" -> true
            qName == "java.lang.CharSequence" -> true
            qName.startsWith("java.lang.") && isWrapperOrNumber(qName) -> true
            qName.startsWith("java.math.") -> true
            qName == "java.util.Date" -> true
            qName == "java.sql.Date" -> true
            qName == "java.sql.Timestamp" -> true
            qName.startsWith("java.time.") -> true
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
        return qName == "java.util.Collection" || qName == "java.util.List" || qName == "java.util.Set" || qName == "java.util.ArrayList" || qName == "java.util.LinkedList" || qName == "java.util.HashSet"
    }
}