package com.sheldon.idea.plugin.api.utils.build.docs
import com.sheldon.idea.plugin.api.utils.build.docs.utils.ApiImplicitParamInfo
import com.sheldon.idea.plugin.api.utils.build.docs.utils.ApiInfo
import com.sheldon.idea.plugin.api.utils.build.docs.utils.ApiModelInfo
import com.sheldon.idea.plugin.api.utils.build.docs.utils.ApiModelPropertyInfo
import com.sheldon.idea.plugin.api.utils.build.docs.utils.ApiOperationInfo
import com.sheldon.idea.plugin.api.utils.build.docs.utils.ApiParamInfo
open class DocInfo(
    var title: String = "",
    var description: String = "",
    var apiInfo: ApiInfo? = ApiInfo(),
    var apiOperationInfo: ApiOperationInfo = ApiOperationInfo(),
    var apiImplicitParamInfo: ApiImplicitParamInfo = ApiImplicitParamInfo(),
    var apiModelInfo: ApiModelInfo = ApiModelInfo(),
    var apiApiModelPropertyInfo: ApiModelPropertyInfo = ApiModelPropertyInfo(),
    var apiParamInfo: ApiParamInfo = ApiParamInfo(),
) {
    /**
     * 核心逻辑：合并新的注释信息
     * @param newTitle 新提取到的标题（例如来自 Swagger）
     * @param newDesc 新提取到的描述
     */
    fun merge(newTitle: String?, newDesc: String?) {
        val safeNewTitle = newTitle?.trim() ?: ""
        val safeNewDesc = newDesc?.trim() ?: ""
        // 如果新来源没有标题，只追加描述（或者什么都不做）
        if (safeNewTitle.isEmpty()) {
            if (safeNewDesc.isNotEmpty()) {
                appendDesc(safeNewDesc)
            }
            return
        }
        // 如果新来源有标题（比如 Swagger 存在），且当前已经有标题（比如 Javadoc 已经解析过）
        // 此时触发【降级逻辑】：将旧的 Title 和 Desc 全部归并到 Desc 中
        if (this.title.isNotEmpty()) {
            val oldContent = "${this.title}\n${this.description}".trim()
            this.description = oldContent
        }
        // 设置新标题
        this.title = safeNewTitle
        // 追加新描述
        if (safeNewDesc.isNotEmpty()) {
            appendDesc(safeNewDesc)
        }
    }
    private fun appendDesc(content: String) {
        if (this.description.isNotEmpty()) {
            this.description += "\n$content"
        } else {
            this.description = content
        }
    }
}