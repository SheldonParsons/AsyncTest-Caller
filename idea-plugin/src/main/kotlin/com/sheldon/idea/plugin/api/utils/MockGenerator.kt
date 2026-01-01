package com.sheldon.idea.plugin.api.utils
import com.google.gson.GsonBuilder
import com.intellij.openapi.module.Module
import com.sheldon.idea.plugin.api.method.AsyncTestBodyType
import com.sheldon.idea.plugin.api.method.AsyncTestVariableNode
import com.sheldon.idea.plugin.api.model.ApiMockRequest
import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.model.FormDataField
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
class MockGenerator(val module: Module) {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    fun generate(
        request: ApiRequest, prefix: String, dsResolver: (String) -> AsyncTestVariableNode? = { key ->
            val cacheService = ProjectCacheService.getInstance(project = module.project)
            cacheService.getDataStructure(module.name, key)?.data?.firstOrNull()
        }
    ): ApiMockRequest {
        return ApiMockRequest(
            path = request.path,
            method = request.method,
            headers = buildHeaders(request.headers),
            query = buildQueryJson(request.query),
            bodyType = request.bodyType,
            body = if (request.bodyType == AsyncTestBodyType.JSON) buildBody(request, dsResolver) else "",
            formData = if (request.bodyType == AsyncTestBodyType.FORM_DATA) buildBody(request, dsResolver) else "",
            prefix = prefix
        )
    }
    private fun buildHeaders(headers: List<AsyncTestVariableNode>): String {
        val headerMap = mutableMapOf<String, String>()
        headers.forEach { node ->
            if (node.name.isNotEmpty()) {
                headerMap[node.name] = node.defaultValue
            }
        }
        return if (headerMap.isEmpty()) "" else gson.toJson(headerMap)
    }
    private fun buildQueryJson(query: List<AsyncTestVariableNode>): String {
        val map = mutableMapOf<String, String>()
        query.forEach { node ->
            if (node.name.isNotEmpty()) {
                map[node.name] = node.defaultValue
            }
        }
        return if (map.isEmpty()) "" else gson.toJson(map)
    }
    private fun buildBody(request: ApiRequest, dsResolver: (String) -> AsyncTestVariableNode?): String {
        return when (request.bodyType) {
            AsyncTestBodyType.JSON -> {
                val rootNode = request.json.firstOrNull() ?: return ""
                val jsonObject = parseNodeValue(rootNode, dsResolver, depth = 0)
                gson.toJson(jsonObject)
            }
            AsyncTestBodyType.FORM_DATA -> {
                val formDataMap = mutableMapOf<String, Any>()
                request.formData.data.forEach { node ->
                    if (node.name.isNotEmpty()) {
                        if (node.type == "files") {
                            formDataMap[node.name] = FormDataField(
                                "file",
                                node.name,
                                "",
                                node.contentType,
                                ArrayList(),
                                node.required
                            )
                        } else {
                            formDataMap[node.name] = FormDataField(
                                "text",
                                node.name,
                                "",
                                node.contentType,
                                ArrayList(),
                                node.required
                            )
                        }
                    }
                }
                gson.toJson(formDataMap)
            }
            AsyncTestBodyType.NONE -> ""
            else -> ""
        }
    }
    private fun parseNodeValue(
        originalNode: AsyncTestVariableNode,
        dsResolver: (String) -> AsyncTestVariableNode?,
        depth: Int
    ): Any? {
        if (depth > 10) return null
        var workingNode = originalNode
        if ((workingNode.type == "ds" || workingNode.type == "object" || workingNode.type == "array")
            && workingNode.children.isEmpty()
            && !workingNode.dsTarget.isNullOrEmpty()
        ) {
            val resolvedNode = dsResolver(workingNode.dsTarget!!)
            if (resolvedNode != null) {
                workingNode = resolvedNode
            }
        }
        return when (workingNode.type) {
            "string" -> workingNode.defaultValue
            "integer" -> workingNode.defaultValue.toIntOrNull() ?: 0
            "number" -> workingNode.defaultValue.toDoubleOrNull() ?: 0.0
            "boolean" -> workingNode.defaultValue.toBoolean()
            "null" -> null
            "ds", "object" -> {
                val map = mutableMapOf<String, Any?>()
                workingNode.children.forEach { child ->
                    if (child.name.isNotEmpty()) {
                        map[child.name] = parseNodeValue(child, dsResolver, depth + 1)
                    }
                }
                map
            }
            "array" -> {
                val list = mutableListOf<Any?>()
                if (workingNode.children.isNotEmpty()) {
                    workingNode.children.forEach { child ->
                        list.add(parseNodeValue(child, dsResolver, depth + 1))
                    }
                } else if (!workingNode.dsTarget.isNullOrEmpty()) {
                    val itemTemplate = dsResolver(workingNode.dsTarget!!)
                    if (itemTemplate != null) {
                        list.add(parseNodeValue(itemTemplate, dsResolver, depth + 1))
                    }
                }
                list
            }
            else -> workingNode.defaultValue
        }
    }
    fun toUrlEncodedQuery(jsonQuery: String?): String {
        if (jsonQuery.isNullOrEmpty()) return ""
        try {
            val map = gson.fromJson(jsonQuery, Map::class.java) ?: return ""
            return map.entries.joinToString("&") { (k, v) ->
                val encodedName = URLEncoder.encode(k.toString(), StandardCharsets.UTF_8.toString())
                val encodedValue = URLEncoder.encode(v.toString(), StandardCharsets.UTF_8.toString())
                "$encodedName=$encodedValue"
            }
        } catch (e: Exception) {
            return ""
        }
    }
}