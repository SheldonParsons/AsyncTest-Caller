package com.sheldon.idea.plugin.api.utils.build.docs.extractor

import com.sheldon.idea.plugin.api.model.CodeType
import com.sheldon.idea.plugin.api.utils.build.docs.DocExtractor
import com.sheldon.idea.plugin.api.utils.build.docs.DocInfo
import com.sheldon.idea.plugin.api.utils.build.docs.ExtractionContext

class SpringDocExtractor : DocExtractor {
    override fun getOrder() = 30
    override fun extract(context: ExtractionContext, currentDoc: DocInfo, codeType: CodeType, hasDocs: Boolean) {
        // TODO: 解析 @Tag, @Operation, @Parameter, @Schema
    }
}