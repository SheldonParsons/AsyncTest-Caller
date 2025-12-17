package com.sheldon.idea.plugin.api.model

import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import com.intellij.util.xmlb.annotations.Transient

/**
 * 对应你要求的 JSON 结构
 */
@Tag("node")
data class ApiNode(
    var type: Int = 0,
    var child_type: Int = 0,
    var code_type: Int = 0,
    var count: Int = 0,
    var method: String? = null,
    var name: String = "",
    var alias: String? = null,
    var desc: String? = null,
    var treePath: String = "",
    @XCollection(propertyElementName = "child")
    var children: ArrayList<ApiNode> = arrayListOf(),
    var request: String? = null,
    @Transient
    var classRequest: ApiRequest? = null,
    var path: String? = null,
    var hash: String = ""
) {
    fun addChild(node: ApiNode) {
        children.add(node)
    }
}