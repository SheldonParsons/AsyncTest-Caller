package com.sheldon.idea.plugin.api.model

import com.sheldon.idea.plugin.api.method.AsyncTestBodyType
import com.sheldon.idea.plugin.api.method.AsyncTestVariableNode

/**
 * 对应你要求的 JSON 结构
 */
data class ApiNode(
    var type: Int,
    var child_type: Int,
    var code_type: Int,
    var count: Int = 0,
    var method: String? = null,
    var name: String,
    var alias: String? = null,
    var desc: String? = null,
    var treePath: String,
    var children: MutableList<ApiNode>? = null,
    var request: String? = null,
    var mock: Any? = null,
    var classRequest: ApiRequest? = null,
    var path: String? = null,
    var hash: String = ""
) {
    fun addChild(node: ApiNode) {
        if (children == null) {
            children = mutableListOf()
        }
        children?.add(node)
    }
}

data class ApiRequest(
    var path: String? = null,
    var method: String? = null,
    var headers: MutableList<AsyncTestVariableNode> = mutableListOf(),
    var query: MutableList<AsyncTestVariableNode> = mutableListOf(),
    var bodyType: String? = "none",
    var formData: AsyncTestFormData = AsyncTestFormData(),
    var urlencoded: MutableList<AsyncTestVariableNode> = mutableListOf(),
    var json: MutableList<AsyncTestVariableNode> = mutableListOf(),
)

data class ApiMockRequest(
    var path: String? = null,
    var method: String? = null,
    var headers: String? = "",
    var query: String? = "",
    var bodyType: String? = "none",
    var body: String = ""
)

data class DataStructure(
    var refer: String? = null,
    var alias: String = "",
    var statement: String = "",
    var data: MutableList<AsyncTestVariableNode> = mutableListOf(),
    var hash: String = ""
)

data class AsyncTestFormData(var boundary: String = "", var data: MutableList<AsyncTestVariableNode> = mutableListOf())


enum class NodeType(val code: Int) {
    INTERFACE(0), CASE(1)
}


enum class ChildNodeType(val code: Int) {
    ROOT_DIR(0), DIR(1), INTERFACE_NODE(2)
}

enum class CodeType(val code: Int) {
    MODULE(0), DIR(1), CLASS(2), METHOD(3);
}