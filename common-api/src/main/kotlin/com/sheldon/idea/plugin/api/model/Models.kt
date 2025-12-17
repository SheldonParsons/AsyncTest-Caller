package com.sheldon.idea.plugin.api.model

import com.sheldon.idea.plugin.api.method.AsyncTestBodyType
import com.sheldon.idea.plugin.api.method.AsyncTestVariableNode

data class ApiRequest(
    var path: String? = null,
    var method: String? = null,
    var headers: MutableList<AsyncTestVariableNode> = mutableListOf(),
    var query: MutableList<AsyncTestVariableNode> = mutableListOf(),
    var bodyType: String? = AsyncTestBodyType.NONE,
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
    var body: String = "",
    var prefix: String = ""
)

data class DataStructure(
    var alias: String = "",
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