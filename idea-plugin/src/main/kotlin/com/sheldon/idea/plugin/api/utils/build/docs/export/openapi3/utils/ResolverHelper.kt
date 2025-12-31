package com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.utils

import com.intellij.openapi.application.ApplicationManager
import com.sheldon.idea.plugin.api.method.AsyncTestType
import com.sheldon.idea.plugin.api.method.AsyncTestVariableNode
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.model.DataStructure
import com.sheldon.idea.plugin.api.utils.GlobalObjectStorageService
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.*

fun resolveHttpMethod(method: String?): PathItem.HttpMethod? {
    if (method.isNullOrBlank()) return null

    return when (method.trim().uppercase()) {
        "GET" -> PathItem.HttpMethod.GET
        "POST" -> PathItem.HttpMethod.POST
        "PUT" -> PathItem.HttpMethod.PUT
        "DELETE" -> PathItem.HttpMethod.DELETE
        "PATCH" -> PathItem.HttpMethod.PATCH
        "HEAD" -> PathItem.HttpMethod.HEAD
        "OPTIONS" -> PathItem.HttpMethod.OPTIONS
        "TRACE" -> PathItem.HttpMethod.TRACE
        else -> null
    }
}

fun getRequest(methodNode: ApiNode): ApiRequest? {
    val cacheService = ApplicationManager.getApplication().getService(GlobalObjectStorageService::class.java)
    val key = "${methodNode.method!!.lowercase()}:${methodNode.path}"
    return cacheService.get<ApiRequest>(key)
}

fun getDsNode(node: AsyncTestVariableNode): AsyncTestVariableNode? {
    val cacheService = ApplicationManager.getApplication().getService(GlobalObjectStorageService::class.java)
    val dataStructure = cacheService.get<DataStructure>(node.dsTarget ?: "")
    if (dataStructure != null && dataStructure.data.isNotEmpty()) {
        return dataStructure.data.first()
    }
    return null
}

fun buildSchemaByAsyncType(type: String): Schema<*> {
    return when (type) {
        AsyncTestType.STRING ->
            StringSchema()

        AsyncTestType.INTEGER ->
            IntegerSchema()

        AsyncTestType.NUMBER ->
            NumberSchema()

        AsyncTestType.BOOLEAN ->
            BooleanSchema()

        AsyncTestType.ARRAY ->
            ArraySchema()

        AsyncTestType.OBJECT ->
            ObjectSchema()

        AsyncTestType.FILES ->
            StringSchema().format("binary")

        AsyncTestType.NULL ->
            Schema<Any>().nullable(true)

        AsyncTestType.DS ->
            Schema<Any>()

        else ->
            Schema<Any>()
    }
}