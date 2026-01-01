package com.sheldon.idea.plugin.api.utils.build.docs
import com.sheldon.idea.plugin.api.model.CodeType
interface DocExtractor {
    /**
     * 提取逻辑
     * @param context 上下文
     * @param currentDoc 当前已提取的文档（用于原地修改）
     */
    fun extract(context: ExtractionContext, currentDoc: DocInfo, codeType: CodeType, hasDocs: Boolean = false)
    // 用于排序，比如 Javadoc 应该先执行，Swagger 后执行
    fun getOrder(): Int
}