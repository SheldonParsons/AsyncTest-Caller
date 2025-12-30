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
    var hash: String = "",
    var name: String = "",
    var alias: String = "",
    var desc: String = ""
)

data class ApiMockRequest(
    var path: String? = null,
    var method: String? = null,
    var headers: String? = "",
    var query: String? = "",
    var bodyType: String? = "none",
    var body: String = "",
    var formData: String = "",
    var prefix: String = "",
    var responseBody: String = "",
    var responseHeaders: String? = "",
)


data class CollectNodeData(
    val url: String,
    val token: String,
    val projectInfo: ModuleSetting,
    val dsMapping: Map<String, String>,
    val apiNode: ApiNode,
    val isModule: Boolean,
    val module: String
)

data class AsyncTestSyncTree(
    val tree: ApiNode,
    val ds_mapping: Map<String, String>,
    val project_id: String,
    val is_module: Boolean
)

data class AsyncTestUpdateDataRequest(
    val request_mapping: MutableMap<String, ApiRequest>,
    val ds_mapping: MutableMap<String, DataStructure>,
    val delete_request: MutableList<String>,
    val project_id: String,
    val is_module: Boolean
)

data class AstResponse<T>(
    val result: Int,
    val msg: T
)

data class AsyncTestDataResponse(
    val tree_action: AstActionTreeResponse,
    val ds_change_list: List<String>
)

data class AstActionTreeResponse(
    val to_create: MutableMap<String, String>,
    val to_update: MutableMap<String, String>,
    val to_delete: MutableList<String>
)

data class DataStructure(
    var alias: String = "",
    var data: MutableList<AsyncTestVariableNode> = mutableListOf(),
    var hash: String = ""
)

data class FormDataField(
    var type: String = "file",
    var name: String = "",
    var value: String = "",
    var contentType: String = "",
    var fileList: ArrayList<String> = ArrayList<String>(),
    var required: Boolean = false
)

//data class AstDataStructure

data class AsyncTestFormData(var boundary: String = "", var data: MutableList<AsyncTestVariableNode> = mutableListOf())


enum class NodeType(val code: Int) {
    INTERFACE(0), CASE(1)
}


enum class ChildNodeType(val code: Int) {
    ROOT_DIR(0), DIR(1), INTERFACE_NODE(2)
}

enum class CodeType(val code: Int) {
    MODULE(0), DIR(1), CLASS(2), METHOD(3), PARAM(4), POJO_CLASS(5), POJO_FIELD(6);
}