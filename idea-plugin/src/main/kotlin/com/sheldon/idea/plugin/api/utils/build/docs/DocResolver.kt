package com.sheldon.idea.plugin.api.utils.build.docs
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParameter
import com.sheldon.idea.plugin.api.model.CodeType
import com.sheldon.idea.plugin.api.utils.build.docs.extractor.JavadocExtractor
import com.sheldon.idea.plugin.api.utils.build.docs.extractor.SpringDocExtractor
import com.sheldon.idea.plugin.api.utils.build.docs.extractor.SwaggerExtractor
import com.sheldon.idea.plugin.api.utils.build.docs.utils.ImplicitParamResolver
class DocResolver {
    // 初始化责任链
    private val chains = listOf(
        JavadocExtractor(),
        SwaggerExtractor(),
        SpringDocExtractor()
    ).sortedBy { it.getOrder() }
    /**
     * 对外暴露的主入口
     */
    fun resolve(
        element: PsiElement,
        parentContextMeta: MutableMap<String, DocInfo> = mutableMapOf(),
        codeType: CodeType,
        hasDocs: Boolean = false
    ): Pair<DocInfo, MutableMap<String, DocInfo>> {
        val result = DocInfo()
        // 1. 如果是方法，先解析隐式参数，准备给参数解析时使用
        var currentMeta = parentContextMeta
        if (isParam(codeType, element)) {
            val implicitParams = ImplicitParamResolver.resolve(element)
            currentMeta = implicitParams
        }
        // 2. 构建上下文
        val context = ExtractionContext(element, currentMeta)
        // 3. 执行责任链
        for (extractor in chains) {
            try {
                extractor.extract(context, result, codeType, hasDocs)
            } catch (e: Exception) {
                // 容错处理，防止某个解析器崩坏影响整体
                e.printStackTrace()
            }
        }
        return Pair(result, context.paramMetadata)
    }
    private fun isParam(codeType: CodeType, element: PsiElement): Boolean {
        return codeType == CodeType.PARAM && element is PsiParameter
    }
}